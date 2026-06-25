package sn.dgcpt.missionsparc.mission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.ChefPosteRepository;
import sn.dgcpt.missionsparc.repository.MissionRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MissionService {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private final MissionRepository missionRepo;
    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;
    private final ChefPosteRepository chefPosteRepo;
    private final ReferentielService referentiel;
    private final CanevasWriter canevasWriter;

    public MissionService(MissionRepository missionRepo, PosteRepository posteRepo, AgentRepository agentRepo,
                          ChefPosteRepository chefPosteRepo, ReferentielService referentiel, CanevasWriter canevasWriter) {
        this.missionRepo = missionRepo;
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
        this.chefPosteRepo = chefPosteRepo;
        this.referentiel = referentiel;
        this.canevasWriter = canevasWriter;
    }

    @Transactional
    public Mission creer(CreationMissionForm f) {
        LocalDate debut = parseDate(f.getDateDebut(), LocalDate.now());
        LocalDate fin = parseDate(f.getDateFin(), null);
        if (fin != null && fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
        }

        List<String> membres = (f.getMembres() == null) ? List.of()
                : f.getMembres().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().collect(Collectors.toList());
        if (membres.isEmpty()) {
            throw new IllegalArgumentException("Sélectionnez au moins un membre pour la mission.");
        }

        String chefMat = (f.getChefMissionSel() == null) ? "" : f.getChefMissionSel().trim();
        if (chefMat.isEmpty() || !membres.contains(chefMat)) {
            throw new IllegalArgumentException("Désignez le chef de mission parmi les membres sélectionnés (un seul).");
        }

        verifierChevauchement(membres, debut, fin);

        Poste poste = (f.getPosteId() != null)
                ? posteRepo.findById(f.getPosteId()).orElseThrow()
                : referentiel.resoudrePoste(f.getCodePoste(), f.getNomPoste());

        Agent chefMission = agentRepo.findById(chefMat).orElseThrow();
        Agent chefPoste = resoudreChefPoste(f, poste);

        Mission m = new Mission();
        m.setReference(genererReference());
        m.setObjet((f.getObjet() == null || f.getObjet().isBlank()) ? "(mission)" : f.getObjet().trim());
        m.setDateDebut(debut);
        m.setDateFin(fin);
        m.setPoste(poste);
        m.setChefMission(chefMission);
        m.setChefPosteFige(chefPoste);
        m.setStatut(StatutMission.EN_CONSOLIDATION);
        for (String mat : membres) {
            agentRepo.findById(mat).ifPresent(m.getMembres()::add);
        }
        Mission saved = missionRepo.save(m);

        historiserChefPoste(poste, chefPoste, debut);
        return saved;
    }

    private void verifierChevauchement(List<String> membres, LocalDate debut, LocalDate fin) {
        verifierChevauchement(membres, debut, fin, null);
    }

    private void verifierChevauchement(List<String> membres, LocalDate debut, LocalDate fin, Integer exclureMissionId) {
        LocalDate finEff = (fin != null) ? fin : LocalDate.of(9999, 12, 31);
        List<String> conflits = new ArrayList<>();
        for (String mat : membres) {
            for (Mission c : missionRepo.membreEnConflit(mat, debut, finEff)) {
                if (exclureMissionId != null && c.getId().equals(exclureMissionId)) continue;
                String nom = agentRepo.findById(mat).map(a -> (a.getNom() + " " + a.getPrenom()).trim()).orElse("");
                String objet = (c.getObjet() == null || c.getObjet().isBlank()) ? "" : " « " + c.getObjet().trim() + " »";
                String tpr = (c.getPoste() == null || c.getPoste().getNom() == null) ? "" : " au TPR " + c.getPoste().getNom();
                conflits.add(mat + (nom.isEmpty() ? "" : " — " + nom) + " (déjà sur " + c.getReference() + objet + tpr + ")");
                break;
            }
        }
        if (!conflits.isEmpty()) {
            throw new IllegalArgumentException(
                    "Chevauchement de période — ces agents sont déjà membres d'une mission sur la même période : "
                    + String.join(" ; ", conflits)
                    + ". Retirez-les d'abord de leur mission existante avant de les ajouter ici.");
        }
    }

    @Transactional(readOnly = true)
    public EditionMissionForm formulaireEdition(Integer id) {
        Mission m = missionRepo.findById(id).orElseThrow();
        EditionMissionForm f = new EditionMissionForm();
        f.setId(m.getId());
        f.setObjet(m.getObjet());
        f.setDateDebut(m.getDateDebut() == null ? "" : m.getDateDebut().toString());
        f.setDateFin(m.getDateFin() == null ? "" : m.getDateFin().toString());
        f.setObservations(m.getObservations());
        f.setStatut(m.getStatut().name());
        f.setChefMissionSel(m.getChefMission() == null ? "" : m.getChefMission().getMatricule());
        f.setMembres(m.getMembres().stream().map(Agent::getMatricule).collect(Collectors.toList()));
        return f;
    }

    @Transactional
    public Mission modifier(EditionMissionForm f) {
        Mission m = missionRepo.findById(f.getId()).orElseThrow();
        LocalDate debut = parseDate(f.getDateDebut(), m.getDateDebut());
        LocalDate fin = parseDate(f.getDateFin(), null);
        if (fin != null && fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
        }
        List<String> membres = (f.getMembres() == null) ? List.of()
                : f.getMembres().stream().filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().collect(Collectors.toList());
        if (membres.isEmpty()) {
            throw new IllegalArgumentException("Sélectionnez au moins un membre pour la mission.");
        }
        String chefMat = (f.getChefMissionSel() == null) ? "" : f.getChefMissionSel().trim();
        if (chefMat.isEmpty() || !membres.contains(chefMat)) {
            throw new IllegalArgumentException("Désignez le chef de mission parmi les membres sélectionnés (un seul).");
        }
        verifierChevauchement(membres, debut, fin, m.getId());

        m.setObjet((f.getObjet() == null || f.getObjet().isBlank()) ? m.getObjet() : f.getObjet().trim());
        m.setDateDebut(debut);
        m.setDateFin(fin);
        m.setObservations((f.getObservations() == null || f.getObservations().isBlank()) ? null : f.getObservations().trim());
        if (f.getStatut() != null && !f.getStatut().isBlank()) {
            m.setStatut(StatutMission.valueOf(f.getStatut()));
        }
        m.setChefMission(agentRepo.findById(chefMat).orElseThrow());
        m.getMembres().clear();
        for (String mat : membres) {
            agentRepo.findById(mat).ifPresent(m.getMembres()::add);
        }
        return missionRepo.save(m);
    }

    @Transactional
    public void cloturer(Integer id) {
        Mission m = missionRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Mission introuvable."));
        m.setStatut(StatutMission.CLOTUREE);
        missionRepo.save(m);
    }

    @Transactional
    public void retirerMembre(Integer missionId, String matricule) {
        Mission m = missionRepo.findById(missionId).orElseThrow();
        m.getMembres().removeIf(a -> a.getMatricule().equals(matricule));
        missionRepo.save(m);
    }

    @Transactional(readOnly = true)
    public List<AgentOption> informaticiens() {
        return agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN).stream().map(this::option).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, TprInfo> tprData() {
        Map<String, TprInfo> data = new LinkedHashMap<>();
        for (Poste p : posteRepo.findAll()) {
            List<AgentOption> agents = agentRepo.findByPoste_Id(p.getId()).stream().map(this::option).collect(Collectors.toList());
            String dernier = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(p.getId())
                    .map(cp -> cp.getAgent().getMatricule()).orElse("");
            data.put(String.valueOf(p.getId()), new TprInfo(agents, dernier));
        }
        return data;
    }

    @Transactional(readOnly = true)
    public byte[] genererCanevas(Integer missionId) throws IOException {
        return canevasWriter.prestamper(missionRepo.findById(missionId).orElseThrow());
    }

    /**
     * Un canevas par agent membre (informaticien), regroupés dans un ZIP : chaque fichier porte le nom
     * de l'agent et a son matricule pré-renseigné comme agent saisisseur (cellule B11 de « 1-Mission et Réseau »).
     */
    @Transactional(readOnly = true)
    public byte[] genererCanevasZip(Integer missionId) throws IOException {
        Mission m = missionRepo.findById(missionId).orElseThrow();
        List<Agent> membres = new ArrayList<>(m.getMembres());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            if (membres.isEmpty()) {
                zip.putNextEntry(new ZipEntry("Canevas-" + m.getReference() + ".xlsx"));
                zip.write(canevasWriter.prestamper(m));
                zip.closeEntry();
            } else {
                for (Agent a : membres) {
                    String nom = (nz(a.getNom()) + " " + nz(a.getPrenom())).trim();
                    String fichier = nettoyer("Canevas-" + m.getReference() + "-" + a.getMatricule() + "-" + nom) + ".xlsx";
                    zip.putNextEntry(new ZipEntry(fichier));
                    zip.write(canevasWriter.prestamper(m, a));
                    zip.closeEntry();
                }
            }
        }
        return bos.toByteArray();
    }

    private String nz(String s) { return s == null ? "" : s; }
    private String nettoyer(String s) { return s.replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", "-"); }

    @Transactional(readOnly = true)
    public String reference(Integer missionId) {
        return missionRepo.findById(missionId).map(Mission::getReference).orElse("mission");
    }

    private AgentOption option(Agent a) {
        return new AgentOption(a.getMatricule(), a.getMatricule() + " — " + a.getNom() + " " + a.getPrenom());
    }

    private Agent resoudreChefPoste(CreationMissionForm f, Poste poste) {
        if (f.getChefPosteSel() != null && !f.getChefPosteSel().isBlank()) {
            return agentRepo.findById(f.getChefPosteSel().trim()).orElseThrow();
        }
        final String fm = (f.getChefPosteMat() == null) ? "" : f.getChefPosteMat().trim();
        if (fm.isEmpty()) {
            throw new IllegalArgumentException("Choisissez un chef de poste existant ou renseignez le matricule du nouvel agent.");
        }
        final String fnom = f.getChefPosteNom(), fprenom = f.getChefPostePrenom();
        return agentRepo.findById(fm).orElseGet(() -> {
            Agent a = new Agent();
            a.setMatricule(fm);
            a.setNom((fnom == null || fnom.isBlank()) ? fm : fnom.trim());
            a.setPrenom((fprenom == null || fprenom.isBlank()) ? "-" : fprenom.trim());
            a.setTypeAgent(TypeAgent.POSTE);
            a.setPoste(poste);
            return agentRepo.save(a);
        });
    }

    private void historiserChefPoste(Poste poste, Agent chef, LocalDate debut) {
        Optional<ChefPoste> courant = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(poste.getId());
        if (courant.isPresent()) {
            if (courant.get().getAgent().getMatricule().equals(chef.getMatricule())) return;
            courant.get().setDateFin(debut);
            chefPosteRepo.saveAndFlush(courant.get());
        }
        ChefPoste cp = new ChefPoste();
        cp.setPoste(poste);
        cp.setAgent(chef);
        cp.setDateDebut(debut);
        chefPosteRepo.save(cp);
    }

    private String genererReference() {
        String prefixe = "MIS-" + Year.now().getValue() + "-";
        long n = missionRepo.countByReferenceStartingWith(prefixe) + 1;
        return prefixe + String.format("%03d", n);
    }

    private LocalDate parseDate(String s, LocalDate defaut) {
        if (s == null || s.isBlank()) return defaut;
        String v = s.trim();
        for (DateTimeFormatter f : FORMATS) {
            try { return LocalDate.parse(v, f); } catch (DateTimeParseException ignored) { }
        }
        return defaut;
    }
}
