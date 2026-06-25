package sn.dgcpt.missionsparc.importation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.LotImport;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutLot;
import sn.dgcpt.missionsparc.importation.dto.*;
import sn.dgcpt.missionsparc.repository.LotImportRepository;
import sn.dgcpt.missionsparc.repository.MissionRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Consolidation des fichiers (lots) d'une mission : détection des conflits, arbitrage, intégration. */
@Service
public class ConsolidationService {

    private final IntegrationService integration;
    private final MissionRepository missionRepo;
    private final LotImportRepository lotRepo;
    private final CanevasReader reader;

    public ConsolidationService(IntegrationService integration, MissionRepository missionRepo,
                                LotImportRepository lotRepo, CanevasReader reader) {
        this.integration = integration;
        this.missionRepo = missionRepo;
        this.lotRepo = lotRepo;
        this.reader = reader;
    }

    @Transactional
    public Integer creerLot(CanevasImporte cv, byte[] bytes, String filename) {
        String ref = (cv.getEntete().getReference() == null) ? "" : cv.getEntete().getReference().trim();
        Mission mission = missionRepo.findByReference(ref).orElseThrow(() ->
                new IllegalArgumentException("Mission « " + ref + " » introuvable. Créez la mission avant d'importer ce fichier."));
        LotImport lot = new LotImport();
        lot.setMission(mission);
        lot.setAgentSaisisseur(cv.getEntete().getAgentSaisisseur());
        lot.setSourceFichier(filename);
        lot.setDateChargement(Instant.now());
        lot.setFichier(bytes);
        lot.setStatut(StatutLot.EN_ATTENTE);
        lotRepo.save(lot);
        return mission.getId();
    }

    @Transactional(readOnly = true)
    public String reference(Integer missionId) {
        return missionRepo.findById(missionId).map(Mission::getReference).orElse("");
    }

    @Transactional(readOnly = true)
    public List<LotVue> lots(Integer missionId) {
        return enAttente(missionId).stream().map(l -> {
            int n = 0;
            try { CanevasImporte c = parse(l);
                n = c.getOrdinateurs().size() + c.getImprimantes().size() + c.getEquipementsReseau().size() + c.getScanners().size();
            } catch (Exception ignored) { }
            String d = (l.getDateChargement() == null) ? "" : l.getDateChargement().toString().substring(0, 10);
            return new LotVue(l.getId(), nz(l.getAgentSaisisseur()), nz(l.getSourceFichier()), d, n);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Conflit> conflits(Integer missionId) {
        List<LotImport> lots = enAttente(missionId);
        return conflitsDe(lots, parserTolerant(lots));
    }

    /** Parse chaque lot une seule fois (tolérant : un lot illisible est ignoré). */
    private Map<Integer, CanevasImporte> parserTolerant(List<LotImport> lots) {
        Map<Integer, CanevasImporte> parsed = new LinkedHashMap<>();
        for (LotImport lot : lots) {
            try { parsed.put(lot.getId(), parse(lot)); } catch (Exception ignored) { }
        }
        return parsed;
    }

    private List<Conflit> conflitsDe(List<LotImport> lots, Map<Integer, CanevasImporte> parsed) {
        Map<String, LinkedHashMap<Integer, Item>> parCle = new LinkedHashMap<>();
        for (LotImport lot : lots) {
            CanevasImporte cv = parsed.get(lot.getId());
            if (cv == null) continue;
            for (Item it : items(cv)) {
                if (it.cle == null) continue;
                parCle.computeIfAbsent(it.cle, k -> new LinkedHashMap<>()).putIfAbsent(lot.getId(), it);
            }
        }
        Map<Integer, LotImport> lotsById = lots.stream().collect(Collectors.toMap(LotImport::getId, l -> l));
        List<Conflit> conflits = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<Integer, Item>> e : parCle.entrySet()) {
            LinkedHashMap<Integer, Item> parLot = e.getValue();
            long sigs = parLot.values().stream().map(i -> i.sig).distinct().count();
            if (parLot.size() >= 2 && sigs > 1) {
                List<OptionConflit> opts = new ArrayList<>();
                String type = "";
                for (Map.Entry<Integer, Item> le : parLot.entrySet()) {
                    Item it = le.getValue(); type = it.type;
                    LotImport lot = lotsById.get(le.getKey());
                    String label = "Lot #" + le.getKey() + " — " + nz(lot.getAgentSaisisseur()) + " (" + nz(lot.getSourceFichier()) + ")";
                    opts.add(new OptionConflit(le.getKey(), label, it.resume));
                }
                conflits.add(new Conflit(e.getKey(), type, opts));
            }
        }
        return conflits;
    }

    @Transactional
    public int integrer(Integer missionId, Map<String, Integer> arbitrages) {
        List<LotImport> lots = enAttente(missionId);
        if (lots.isEmpty()) throw new IllegalStateException("Aucun fichier en attente pour cette mission.");
        // Parse chaque lot UNE seule fois (réutilisé pour la détection des conflits et l'intégration).
        Map<Integer, CanevasImporte> parsed = new LinkedHashMap<>();
        for (LotImport lot : lots) {
            try { parsed.put(lot.getId(), parse(lot)); }
            catch (IOException e) { throw new IllegalStateException("Lecture du lot #" + lot.getId() + " impossible."); }
        }
        List<Conflit> conflits = conflitsDe(lots, parsed);
        Map<String, Integer> gagnants = new HashMap<>(arbitrages == null ? Map.of() : arbitrages);
        Set<String> clesConflit = new HashSet<>();
        for (Conflit c : conflits) {
            clesConflit.add(c.getCle());
            gagnants.putIfAbsent(c.getCle(), c.getOptions().get(0).getLotId());
        }
        int total = 0;
        for (LotImport lot : lots) {
            CanevasImporte cv = parsed.get(lot.getId());
            filtrer(cv, lot.getId(), clesConflit, gagnants);
            total += integration.integrer(cv);
            lot.setStatut(StatutLot.INTEGRE);
            lotRepo.save(lot);
        }
        return total;
    }

    @Transactional
    public Integer supprimerLot(Integer lotId) {
        LotImport lot = lotRepo.findById(lotId).orElseThrow();
        Integer missionId = lot.getMission().getId();
        lotRepo.delete(lot);
        return missionId;
    }

    // ---------- interne ----------

    private List<LotImport> enAttente(Integer missionId) {
        return lotRepo.findByMission_IdAndStatut(missionId, StatutLot.EN_ATTENTE);
    }

    private CanevasImporte parse(LotImport lot) throws IOException {
        return reader.lire(new ByteArrayInputStream(lot.getFichier()));
    }

    private void filtrer(CanevasImporte cv, Integer lotId, Set<String> clesConflit, Map<String, Integer> gagnants) {
        cv.getOrdinateurs().removeIf(o -> retirer(cleOrd(o), lotId, clesConflit, gagnants));
        cv.getImprimantes().removeIf(i -> retirer(cleImp(i), lotId, clesConflit, gagnants));
        cv.getEquipementsReseau().removeIf(eq -> retirer(cleReseau(eq), lotId, clesConflit, gagnants));
        cv.getScanners().removeIf(sc -> retirer(cleScan(sc), lotId, clesConflit, gagnants));
    }

    private boolean retirer(String cle, Integer lotId, Set<String> clesConflit, Map<String, Integer> gagnants) {
        return cle != null && clesConflit.contains(cle) && !lotId.equals(gagnants.get(cle));
    }

    private List<Item> items(CanevasImporte cv) {
        List<Item> items = new ArrayList<>();
        for (LigneOrdinateur o : cv.getOrdinateurs())
            items.add(new Item(cleOrd(o), "Ordinateur", sigOrd(o), resume(o.getNumeroInventaire(), o.getMacEthernet(), o.getNomMachine())));
        for (LigneImprimante i : cv.getImprimantes())
            items.add(new Item(cleImp(i), "Imprimante", sigImp(i), resume(i.getNumeroInventaire(), i.getMac(), i.getNom())));
        for (LigneEquipementReseau eq : cv.getEquipementsReseau())
            items.add(new Item(cleReseau(eq), "Réseau", sigReseau(eq), resume(eq.getNumeroInventaire(), eq.getMac(), eq.getNom())));
        for (LigneScanner sc : cv.getScanners())
            items.add(new Item(cleScan(sc), "Scanner", sigScan(sc), resume(sc.getNumeroInventaire(), sc.getNumeroSerie(), sc.getMarque())));
        return items;
    }

    private String cle(String numero, String secondaire) {
        if (numero != null && !numero.isBlank()) return "N:" + numero.trim();
        if (secondaire != null && !secondaire.isBlank()) return "K:" + secondaire.trim().toLowerCase();
        return null;
    }
    private String cleOrd(LigneOrdinateur o) { return cle(o.getNumeroInventaire(), o.getMacEthernet()); }
    private String cleImp(LigneImprimante i) { return cle(i.getNumeroInventaire(), i.getMac()); }
    private String cleReseau(LigneEquipementReseau e) { return cle(e.getNumeroInventaire(), e.getMac()); }
    private String cleScan(LigneScanner s) { return cle(s.getNumeroInventaire(), s.getNumeroSerie()); }

    private String sigOrd(LigneOrdinateur o) {
        return j(o.getNomMachine(), o.getModele(), o.getMacEthernet(), o.getMacWifi(), o.getAgentAttributaire(),
                o.getAgentInstallateur(), b(o.isAster()), b(o.isAntivirus()), b(o.isSicCDD()), b(o.isCic()),
                b(o.isSysbudget()), b(o.isAd()), o.getRam(), o.getProcesseur(), o.getDisqueDur(), o.getStatut(), o.getObservation());
    }
    private String sigImp(LigneImprimante i) { return j(i.getNom(), i.getModele(), i.getMac(), i.getMacWifi(), i.getIp(), i.getStatut(), i.getObservation()); }
    private String sigReseau(LigneEquipementReseau e) { return j(e.getType(), e.getNom(), e.getModele(), e.getMac(), e.getIp(), e.getStatut(), e.getObservation()); }
    private String sigScan(LigneScanner s) { return j(s.getNumeroSerie(), s.getMarque(), s.getModele(), s.getStatut(), s.getObservation()); }

    private String resume(String a, String b, String c) {
        String id = (a != null && !a.isBlank()) ? a : nz(b);
        String r = (nz(id) + " — " + nz(c)).trim();
        return r.endsWith("—") ? r.substring(0, r.length() - 1).trim() : r;
    }
    private String j(String... v) { StringBuilder sb = new StringBuilder(); for (String x : v) sb.append(nz(x)).append("|"); return sb.toString(); }
    private String b(boolean x) { return x ? "1" : "0"; }
    private String nz(String x) { return x == null ? "" : x; }

    private static class Item {
        final String cle, type, sig, resume;
        Item(String cle, String type, String sig, String resume) { this.cle = cle; this.type = type; this.sig = sig; this.resume = resume; }
    }
}
