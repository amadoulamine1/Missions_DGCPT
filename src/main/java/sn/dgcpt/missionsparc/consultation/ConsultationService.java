package sn.dgcpt.missionsparc.consultation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.consultation.dto.*;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Lecture seule : restitutions (postes, parc, missions). Mappe vers des DTO de vue. */
@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private final PosteRepository posteRepo;
    private final MaterielRepository materielRepo;
    private final ChefPosteRepository chefPosteRepo;
    private final AgentRepository agentRepo;
    private final MissionRepository missionRepo;
    private final ReleveMaterielRepository releveRepo;
    private final OrdinateurRepository ordinateurRepo;
    private final ImprimanteRepository imprimanteRepo;
    private final EquipementReseauRepository reseauRepo;
    private final ScannerChequeRepository scannerRepo;
    private final AffectationMaterielRepository affectationRepo;

    public ConsultationService(PosteRepository posteRepo, MaterielRepository materielRepo,
                               ChefPosteRepository chefPosteRepo, AgentRepository agentRepo,
                               MissionRepository missionRepo, ReleveMaterielRepository releveRepo,
                               OrdinateurRepository ordinateurRepo, ImprimanteRepository imprimanteRepo,
                               EquipementReseauRepository reseauRepo, ScannerChequeRepository scannerRepo,
                               AffectationMaterielRepository affectationRepo) {
        this.posteRepo = posteRepo;
        this.materielRepo = materielRepo;
        this.chefPosteRepo = chefPosteRepo;
        this.agentRepo = agentRepo;
        this.missionRepo = missionRepo;
        this.releveRepo = releveRepo;
        this.ordinateurRepo = ordinateurRepo;
        this.imprimanteRepo = imprimanteRepo;
        this.reseauRepo = reseauRepo;
        this.scannerRepo = scannerRepo;
        this.affectationRepo = affectationRepo;
    }

    // ---- Postes ----

    public List<PosteVue> listerPostes() {
        return posteRepo.findAll().stream().map(this::versPosteVue).toList();
    }

    public PosteDetailVue detailPoste(Integer id) {
        Poste p = posteRepo.findById(id).orElseThrow();
        var courant = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(id);
        String chef = courant.map(cp -> cp.getAgent().getMatricule() + " — " + cp.getAgent().getNom()).orElse("(aucun)");
        String chefMat = courant.map(cp -> cp.getAgent().getMatricule()).orElse("");
        List<AgentVue> agents = agentRepo.findByPoste_Id(id).stream().map(this::versAgentVue).toList();
        List<MaterielLignePoste> materiels = materielRepo.findByPoste_Id(id).stream().map(this::versLignePoste).toList();
        return new PosteDetailVue(versPosteVue(p), chef, chefMat, agents, materiels);
    }

    // ---- Parc ----

    public List<MaterielVue> listerParc() {
        return materielRepo.findAll().stream().map(this::versMaterielVue).toList();
    }

    public List<MaterielVue> listerParc(String q, Integer posteId, String type, String statut) {
        String texte = (q == null) ? "" : q.trim().toLowerCase();
        return materielRepo.findAll().stream()
                .filter(m -> posteId == null || (m.getPoste() != null && posteId.equals(m.getPoste().getId())))
                .filter(m -> type == null || type.isBlank() || m.getType().name().equals(type))
                .filter(m -> statut == null || statut.isBlank() || (m.getStatut() != null && m.getStatut().name().equals(statut)))
                .filter(m -> texte.isEmpty() || contientTexte(m, texte))
                .map(this::versMaterielVue)
                .toList();
    }

    private boolean contientTexte(Materiel m, String s) {
        return (m.getNumeroInventaire() != null && m.getNumeroInventaire().toLowerCase().contains(s))
                || (m.getNom() != null && m.getNom().toLowerCase().contains(s))
                || (m.getModele() != null && m.getModele().toLowerCase().contains(s));
    }

    // ---- Missions ----

    public List<MissionVue> listerMissions() {
        return missionRepo.findAll().stream().map(this::versMissionVue).toList();
    }

    public MissionDetailVue detailMission(Integer id) {
        Mission m = missionRepo.findById(id).orElseThrow();
        String chefMission = m.getChefMission() == null ? "" : m.getChefMission().getMatricule() + " — " + m.getChefMission().getPrenom() + " " + m.getChefMission().getNom();
        String chefPoste = m.getChefPosteFige() == null ? "" : m.getChefPosteFige().getMatricule() + " — " + m.getChefPosteFige().getPrenom() + " " + m.getChefPosteFige().getNom();
        String cable = m.getCategorieCable() == null ? "" : m.getCategorieCable().getLibelle();
        List<ReleveVue> releves = releveRepo.findByMission_Id(id).stream().map(this::versReleveVue).toList();
        List<AgentVue> membres = m.getMembres().stream().map(this::versAgentVue).toList();
        return new MissionDetailVue(versMissionVue(m), chefMission, chefPoste, m.getEtatCablage(), cable, m.getObservations(), releves, membres);
    }

    public MaterielDetailVue detailMateriel(String numero) {
        Materiel m = materielRepo.findById(numero).orElseThrow();
        List<String[]> attrs = new ArrayList<>();
        switch (m.getType()) {
            case ORDINATEUR -> ordinateurRepo.findById(numero).ifPresent(o -> {
                attrs.add(new String[]{"Nom machine", nz(o.getNomMachine())});
                attrs.add(new String[]{"MAC ethernet", nz(o.getMacEthernet())});
                attrs.add(new String[]{"MAC wifi", nz(o.getMacWifi())});
                attrs.add(new String[]{"RAM", nz(o.getRam())});
                attrs.add(new String[]{"Processeur", nz(o.getProcesseur())});
                attrs.add(new String[]{"Disque dur", nz(o.getDisqueDur())});
                attrs.add(new String[]{"Agent traitant", o.getAgentInstallateur() == null ? "" :
                        o.getAgentInstallateur().getMatricule() + " — " + o.getAgentInstallateur().getNom() + " " + o.getAgentInstallateur().getPrenom()});
                attrs.add(new String[]{"Logiciels", o.getLogiciels().stream().map(Logiciel::getNom).sorted().collect(Collectors.joining(", "))});
            });
            case IMPRIMANTE -> imprimanteRepo.findById(numero).ifPresent(i -> {
                attrs.add(new String[]{"MAC", nz(i.getMac())});
                attrs.add(new String[]{"MAC wifi", nz(i.getMacWifi())});
                attrs.add(new String[]{"IP", nz(i.getIp())});
            });
            case SWITCH, ACCESS_POINT -> reseauRepo.findById(numero).ifPresent(e -> {
                attrs.add(new String[]{"MAC", nz(e.getMac())});
                attrs.add(new String[]{"IP", nz(e.getIp())});
            });
            case SCANNER_CHEQUE -> scannerRepo.findById(numero).ifPresent(sc -> {
                attrs.add(new String[]{"Numéro de série", nz(sc.getNumeroSerie())});
                attrs.add(new String[]{"Marque", nz(sc.getMarque())});
            });
            default -> { }
        }
        var aff = affectationRepo.findByMaterielAndDateFinIsNull(m);
        String affA = aff.map(a -> a.getAgent() == null ? "" :
                a.getAgent().getMatricule() + " — " + a.getAgent().getNom() + " " + a.getAgent().getPrenom()).orElse("");
        String affP = aff.map(a -> a.getPoste() == null ? "" : a.getPoste().getNom()).orElse("");
        String affD = aff.map(a -> a.getDateDebut() == null ? "" : a.getDateDebut().toString()).orElse("");
        List<String[]> rel = releveRepo.findByMateriel_NumeroInventaire(numero).stream()
                .map(r -> new String[]{
                        r.getMission() == null ? "" : r.getMission().getReference(),
                        r.getDateReleve() == null ? "" : r.getDateReleve().toString(),
                        r.getAgentSaisisseur() == null ? "" : r.getAgentSaisisseur().getMatricule(),
                        nz(r.getZone())
                }).toList();
        List<String[]> histo = affectationRepo.findByMaterielOrderByDateDebutDesc(m).stream()
                .map(a -> new String[]{
                        a.getAgent() == null ? "(poste)" : a.getAgent().getMatricule() + " — " + a.getAgent().getNom() + " " + a.getAgent().getPrenom(),
                        a.getDateDebut() == null ? "" : a.getDateDebut().toString(),
                        a.getDateFin() == null ? "En cours" : a.getDateFin().toString()
                }).toList();
        String cree = m.getDateCreation() == null ? "" : m.getDateCreation().toString().substring(0, 10);
        return new MaterielDetailVue(versMaterielVue(m), cree, attrs, affA, affP, affD, rel, histo);
    }

    private String nz(String s) { return s == null ? "" : s; }

    public List<InventaireDateLigne> inventaireALaDate(LocalDate d) {
        return affectationRepo.actives(d).stream().map(a -> {
            Materiel m = a.getMateriel();
            String affecteA = a.getAgent() == null ? "" :
                    a.getAgent().getMatricule() + " — " + a.getAgent().getNom() + " " + a.getAgent().getPrenom();
            return new InventaireDateLigne(
                    m.getNumeroInventaire(), m.getType().name(), m.getNom(), m.getModele(),
                    a.getPoste() == null ? "" : a.getPoste().getNom(), affecteA,
                    a.getDateDebut() == null ? "" : a.getDateDebut().toString());
        }).toList();
    }

    public List<MissionVue> listerMissions(String q, Integer posteId, String etat) {
        String texte = (q == null) ? "" : q.trim().toLowerCase();
        return missionRepo.findAll().stream()
                .filter(m -> posteId == null || (m.getPoste() != null && posteId.equals(m.getPoste().getId())))
                .filter(m -> etat == null || etat.isBlank() || etatTemporel(m).equals(etat))
                .filter(m -> texte.isEmpty()
                        || (m.getReference() != null && m.getReference().toLowerCase().contains(texte))
                        || (m.getObjet() != null && m.getObjet().toLowerCase().contains(texte)))
                .map(this::versMissionVue)
                .toList();
    }

    public List<ReleveVue> relevesDeMission(Integer id) {
        return releveRepo.findByMission_Id(id).stream().map(this::versReleveVue).toList();
    }

    // ---- mappers ----

    private PosteVue versPosteVue(Poste p) {
        return new PosteVue(p.getId(), p.getCode(), p.getNom(), p.getRegion(), materielRepo.countByPoste_Id(p.getId()));
    }

    private AgentVue versAgentVue(Agent a) {
        return new AgentVue(a.getMatricule(), a.getNom() + " " + a.getPrenom(), a.getFonction(), a.getTelephone());
    }

    private MaterielLignePoste versLignePoste(Materiel m) {
        String statut = libelleStatut(m.getStatut());
        String affecteA = affectationRepo.findByMaterielAndDateFinIsNull(m)
                .map(AffectationMateriel::getAgent)
                .map(a -> a.getMatricule() + " — " + a.getNom() + " " + a.getPrenom()).orElse("");
        return new MaterielLignePoste(m.getNumeroInventaire(), m.getType().name(), m.getNom(), m.getModele(),
                statut, affecteA, caracteristiques(m));
    }

    private String caracteristiques(Materiel m) {
        String num = m.getNumeroInventaire();
        return switch (m.getType()) {
            case ORDINATEUR -> ordinateurRepo.findById(num).map(o ->
                    join(" · ", pre("MAC ", o.getMacEthernet()), nz(o.getRam()), nz(o.getProcesseur()), nz(o.getDisqueDur()))).orElse("");
            case IMPRIMANTE -> imprimanteRepo.findById(num).map(i ->
                    join(" · ", pre("MAC ", i.getMac()), pre("IP ", i.getIp()))).orElse("");
            case SWITCH, ACCESS_POINT -> reseauRepo.findById(num).map(e ->
                    join(" · ", pre("MAC ", e.getMac()), pre("IP ", e.getIp()))).orElse("");
            case SCANNER_CHEQUE -> scannerRepo.findById(num).map(sc ->
                    join(" · ", pre("Série ", sc.getNumeroSerie()), nz(sc.getMarque()))).orElse("");
            default -> "";
        };
    }

    private String pre(String prefixe, String val) {
        return (val == null || val.isBlank()) ? "" : prefixe + val.trim();
    }

    private String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String x : parts) {
            if (x == null || x.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(x.trim());
        }
        return sb.toString();
    }

    public List<AgentVue> candidatsAffectation(String numero) {
        Materiel m = materielRepo.findById(numero).orElse(null);
        if (m == null || m.getPoste() == null) return List.of();
        return agentRepo.findByPoste_Id(m.getPoste().getId()).stream()
                .map(a -> new AgentVue(a.getMatricule(), a.getNom() + " " + a.getPrenom(), a.getFonction(), a.getTelephone()))
                .toList();
    }

    private MaterielVue versMaterielVue(Materiel m) {
        String poste = m.getPoste() == null ? "" : m.getPoste().getNom();
        String statut = libelleStatut(m.getStatut());
        String obs = m.getObservation() == null ? "" : m.getObservation();
        String ram = "", proc = "", disque = "";
        if (m.getType() == TypeMateriel.ORDINATEUR) {
            var o = ordinateurRepo.findById(m.getNumeroInventaire()).orElse(null);
            if (o != null) { ram = nz(o.getRam()); proc = nz(o.getProcesseur()); disque = nz(o.getDisqueDur()); }
        }
        return new MaterielVue(m.getNumeroInventaire(), m.getType().name(), m.getNom(), m.getModele(), poste, statut, obs, ram, proc, disque);
    }

    private String libelleStatut(StatutMateriel s) {
        if (s == null) return "";
        return switch (s) {
            case EN_SERVICE -> "En service";
            case EN_PANNE -> "En panne";
            case A_CHANGER -> "À changer";
        };
    }

    private MissionVue versMissionVue(Mission m) {
        String poste = m.getPoste() == null ? "" : m.getPoste().getNom();
        return new MissionVue(m.getId(), m.getReference(), m.getObjet(), poste,
                periode(m.getDateDebut(), m.getDateFin()), m.getStatut().name(), etatTemporel(m));
    }

    /** État temporel dérivé des dates : Planifiée (à venir), En cours, Terminée. */
    private String etatTemporel(Mission m) {
        LocalDate today = LocalDate.now();
        LocalDate debut = m.getDateDebut();
        LocalDate fin = m.getDateFin();
        if (debut != null && debut.isAfter(today)) return "Planifiée";
        if (fin == null || !fin.isBefore(today)) return "En cours";
        return "Terminée";
    }

    private ReleveVue versReleveVue(ReleveMateriel r) {
        Materiel m = r.getMateriel();
        String saisisseur = r.getAgentSaisisseur() == null ? "" : r.getAgentSaisisseur().getMatricule();
        return new ReleveVue(m.getNumeroInventaire(), m.getType().name(), m.getNom(), saisisseur, r.getZone(), r.getDateReleve());
    }

    private String periode(LocalDate d1, LocalDate d2) {
        String a = (d1 == null) ? "?" : d1.toString();
        String b = (d2 == null) ? "…" : d2.toString();
        return a + " → " + b;
    }
}
