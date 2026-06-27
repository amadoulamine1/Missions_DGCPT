package sn.dgcpt.missionsparc.consultation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.consultation.dto.RapportAnnuelVue;
import sn.dgcpt.missionsparc.consultation.dto.SerieAnnuelle;
import sn.dgcpt.missionsparc.consultation.dto.StatPoste;
import sn.dgcpt.missionsparc.consultation.dto.TypeStat;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.IntToLongFunction;
import java.util.stream.Collectors;

/**
 * Revue annuelle des missions et du parc (rapport de pilotage). Agrège en mémoire (volume cohérent
 * avec le reste de l'application) les indicateurs d'une année, leur tendance sur ≤ 5 ans et une
 * prévision N+1 (cf. {@link Prevision}).
 */
@Service
public class RapportAnnuelService {

    private static final TypeMateriel[][] FAMILLES = {
            {TypeMateriel.ORDINATEUR}, {TypeMateriel.IMPRIMANTE},
            {TypeMateriel.SWITCH, TypeMateriel.ACCESS_POINT}, {TypeMateriel.SCANNER_CHEQUE}
    };
    private static final String[] FAMILLES_LIB = {"Ordinateurs", "Imprimantes", "Switchs / AP", "Scanners chèque"};

    private final MaterielRepository materielRepo;
    private final MissionRepository missionRepo;
    private final ReleveMaterielRepository releveRepo;
    private final AffectationMaterielRepository affectationRepo;

    public RapportAnnuelService(MaterielRepository materielRepo, MissionRepository missionRepo,
                                ReleveMaterielRepository releveRepo, AffectationMaterielRepository affectationRepo) {
        this.materielRepo = materielRepo;
        this.missionRepo = missionRepo;
        this.releveRepo = releveRepo;
        this.affectationRepo = affectationRepo;
    }

    @Transactional(readOnly = true)
    public RapportAnnuelVue rapport(int annee, int ans) {
        int fenetre = Math.max(2, Math.min(5, ans));
        int anneeCourante = LocalDate.now().getYear();

        List<Materiel> materiels = materielRepo.findAll();
        List<Mission> missions = missionRepo.findAll();
        List<ReleveMateriel> releves = releveRepo.findAll();
        List<AffectationMateriel> affectations = affectationRepo.findAll();

        // Relevés groupés par matériel, triés du plus récent (pour le statut « à une date »)
        Map<String, List<ReleveMateriel>> relevesParMat = releves.stream()
                .filter(r -> r.getMateriel() != null)
                .collect(Collectors.groupingBy(r -> r.getMateriel().getNumeroInventaire()));
        relevesParMat.values().forEach(l -> l.sort(Comparator.comparing(
                ReleveMateriel::getDateReleve, Comparator.nullsLast(Comparator.reverseOrder()))));

        RapportAnnuelVue v = new RapportAnnuelVue();
        v.setAnnee(annee);
        v.setAns(fenetre);
        v.setAnneesDisponibles(anneesDisponibles(missions, materiels, anneeCourante));

        // ---- Indicateurs de pilotage : séries (≤ 5 ans) + prévision N+1 ----
        IntToLongFunction nbMissions = y -> missions.stream().filter(m -> anneeDe(m.getDateDebut()) == y).count();
        IntToLongFunction nbReleves  = y -> releves.stream().filter(r -> anneeDe(r.getDateReleve()) == y).count();
        IntToLongFunction nbIncidents = y -> releves.stream()
                .filter(r -> anneeDe(r.getDateReleve()) == y && estIncident(r.getStatutObserve())).count();
        IntToLongFunction nbNouveau  = y -> materiels.stream().filter(m -> anneeDe(m.getDateCreation()) == y).count();
        IntToLongFunction taille     = y -> materiels.stream().filter(m -> existeAuFin(m, y)).count();
        IntToLongFunction dispo      = y -> dispoAuFin(materiels, relevesParMat, y, anneeCourante);

        List<SerieAnnuelle> series = new ArrayList<>();
        series.add(serie("Missions menées", "", 0, annee, fenetre, nbMissions, null));
        series.add(serie("Relevés effectués", "", 0, annee, fenetre, nbReleves, null));
        series.add(serie("Incidents (pannes / à changer)", "", -1, annee, fenetre, nbIncidents, null));
        series.add(serie("Matériel nouvellement inventorié", "", 0, annee, fenetre, nbNouveau, null));
        series.add(serie("Taille du parc (31/12)", "", 0, annee, fenetre, taille, null));
        series.add(serie("Disponibilité (31/12)", "%", 1, annee, fenetre, dispo, 100L));
        v.setSeries(series);

        // ---- Missions de l'année ----
        List<Mission> missionsAnnee = missions.stream()
                .filter(m -> anneeDe(m.getDateDebut()) == annee)
                .sorted(Comparator.comparing(Mission::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<Integer, Long> relevesParMission = releves.stream()
                .filter(r -> r.getMission() != null)
                .collect(Collectors.groupingBy(r -> r.getMission().getId(), Collectors.counting()));
        List<String[]> lignesMissions = new ArrayList<>();
        long[] parMois = new long[12];
        Map<String, long[]> missParPoste = new LinkedHashMap<>();
        for (Mission m : missionsAnnee) {
            if (m.getDateDebut() != null) parMois[m.getDateDebut().getMonthValue() - 1]++;
            String poste = m.getPoste() == null ? "(sans poste)" : m.getPoste().getNom();
            missParPoste.computeIfAbsent(poste, k -> new long[1])[0]++;
            lignesMissions.add(new String[]{
                    nz(m.getReference()), nz(m.getObjet()), poste,
                    periode(m.getDateDebut(), m.getDateFin()),
                    m.getChefMission() == null ? "" : m.getChefMission().getNom() + " " + m.getChefMission().getPrenom(),
                    String.valueOf(relevesParMission.getOrDefault(m.getId(), 0L)),
                    etatTemporel(m, anneeCourante)
            });
        }
        v.setMissionsAnnee(lignesMissions);
        v.setMissionsParMois(parMois);
        v.setMissionsParPoste(missParPoste.entrySet().stream()
                .map(e -> new StatPoste(e.getKey(), e.getValue()[0], 0))
                .sorted(Comparator.comparingLong(StatPoste::getTotal).reversed()).toList());
        v.setPostesCouverts(missParPoste.size());

        // ---- Parc au 31/12 ----
        List<Materiel> parc = materiels.stream().filter(m -> existeAuFin(m, annee)).toList();
        long svc = 0, pan = 0, chg = 0;
        Map<String, long[]> parcPoste = new LinkedHashMap<>();
        for (Materiel m : parc) {
            StatutMateriel st = statutAuFin(m, relevesParMat, annee, anneeCourante);
            if (st == StatutMateriel.EN_SERVICE) svc++;
            else if (st == StatutMateriel.EN_PANNE) pan++;
            else if (st == StatutMateriel.A_CHANGER) chg++;
            String p = m.getPoste() == null ? "(sans poste)" : m.getPoste().getNom();
            long[] a = parcPoste.computeIfAbsent(p, k -> new long[2]);
            a[0]++;
            if (st == StatutMateriel.EN_PANNE) a[1]++;
        }
        long statues = svc + pan + chg;
        v.setParcTaille(parc.size());
        v.setParcSvc(svc); v.setParcPan(pan); v.setParcChg(chg);
        v.setParcDispo(statues > 0 ? svc * 100 / statues : 0);
        v.setParcParType(parTypeAuFin(parc, relevesParMat, annee, anneeCourante));
        v.setParcParPoste(parcPoste.entrySet().stream()
                .map(e -> new StatPoste(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(StatPoste::getTotal).reversed()).toList());

        // matériel ajouté dans l'année, par type
        List<Materiel> nouveau = materiels.stream().filter(m -> anneeDe(m.getDateCreation()) == annee).toList();
        v.setNouveauTotal(nouveau.size());
        v.setNouveauParType(parTypeSimple(nouveau));

        // ---- Incidents & maintenance ----
        List<ReleveMateriel> incidents = releves.stream()
                .filter(r -> anneeDe(r.getDateReleve()) == annee && estIncident(r.getStatutObserve()) && r.getMateriel() != null)
                .toList();
        v.setIncidentsParType(incidentsParType(incidents));
        Map<String, long[]> incPoste = new LinkedHashMap<>();
        List<String[]> incListe = new ArrayList<>();
        for (ReleveMateriel r : incidents) {
            Materiel m = r.getMateriel();
            String p = m.getPoste() == null ? "(sans poste)" : m.getPoste().getNom();
            long[] a = incPoste.computeIfAbsent(p, k -> new long[2]);
            a[0]++;
            if (r.getStatutObserve() == StatutMateriel.EN_PANNE) a[1]++;
            if (incListe.size() < 100) incListe.add(new String[]{
                    nz(m.getNumeroInventaire()), libelleType(m), nz(m.getNom()), p,
                    libelleStatut(r.getStatutObserve()), r.getDateReleve() == null ? "" : r.getDateReleve().toString()});
        }
        v.setIncidentsParPoste(incPoste.entrySet().stream()
                .map(e -> new StatPoste(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(StatPoste::getTotal).reversed()).toList());
        v.setIncidentsListe(incListe);
        v.setReaffectations(affectations.stream().filter(a -> anneeDe(a.getDateDebut()) == annee).count());

        // ---- Activité des agents (membres des missions de l'année) ----
        Map<String, long[]> activite = new LinkedHashMap<>();
        Map<String, String> nomAgent = new HashMap<>();
        for (Mission m : missionsAnnee) {
            for (Agent a : m.getMembres()) {
                activite.computeIfAbsent(a.getMatricule(), k -> new long[1])[0]++;
                nomAgent.putIfAbsent(a.getMatricule(), (a.getNom() + " " + a.getPrenom()).trim());
            }
        }
        v.setActiviteAgents(activite.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(x -> -x[0])))
                .map(e -> new String[]{e.getKey(), nomAgent.getOrDefault(e.getKey(), ""), String.valueOf(e.getValue()[0])})
                .toList());

        return v;
    }

    // ---------- séries & prévision ----------

    private SerieAnnuelle serie(String lib, String unite, int sens, int annee, int ans, IntToLongFunction metric, Long plafond) {
        List<SerieAnnuelle.Point> pts = new ArrayList<>();
        List<Long> vals = new ArrayList<>();
        for (int y = annee - ans + 1; y <= annee; y++) {
            long val = metric.applyAsLong(y);
            pts.add(new SerieAnnuelle.Point(y, val, false));
            vals.add(val);
        }
        Long prev = Prevision.projeterEntier(vals);
        if (prev != null) {
            if (plafond != null) prev = Math.min(prev, plafond);
            pts.add(new SerieAnnuelle.Point(annee + 1, prev, true));
        }
        return new SerieAnnuelle(lib, unite, sens, pts);
    }

    // ---------- statut « au 31/12 » ----------

    /** Statut du matériel au 31/12/y : dernier relevé ≤ 31/12/y ; repli sur le statut courant pour l'année en cours. */
    private StatutMateriel statutAuFin(Materiel m, Map<String, List<ReleveMateriel>> relevesParMat, int y, int anneeCourante) {
        LocalDate fin = LocalDate.of(y, 12, 31);
        List<ReleveMateriel> rl = relevesParMat.get(m.getNumeroInventaire());
        if (rl != null) {
            for (ReleveMateriel r : rl) { // déjà triés du plus récent
                if (r.getDateReleve() != null && !r.getDateReleve().isAfter(fin) && r.getStatutObserve() != null)
                    return r.getStatutObserve();
            }
        }
        return (y >= anneeCourante) ? m.getStatut() : null; // année passée sans relevé : statut inconnu
    }

    private long dispoAuFin(List<Materiel> materiels, Map<String, List<ReleveMateriel>> relevesParMat, int y, int anneeCourante) {
        long svc = 0, statues = 0;
        for (Materiel m : materiels) {
            if (!existeAuFin(m, y)) continue;
            StatutMateriel st = statutAuFin(m, relevesParMat, y, anneeCourante);
            if (st == null) continue;
            statues++;
            if (st == StatutMateriel.EN_SERVICE) svc++;
        }
        return statues > 0 ? svc * 100 / statues : 0;
    }

    private List<TypeStat> parTypeAuFin(List<Materiel> parc, Map<String, List<ReleveMateriel>> relevesParMat, int y, int anneeCourante) {
        List<TypeStat> out = new ArrayList<>();
        for (int i = 0; i < FAMILLES.length; i++) {
            List<TypeMateriel> fam = Arrays.asList(FAMILLES[i]);
            long total = 0, svc = 0, pan = 0, chg = 0;
            for (Materiel m : parc) {
                if (!fam.contains(m.getType())) continue;
                total++;
                StatutMateriel st = statutAuFin(m, relevesParMat, y, anneeCourante);
                if (st == StatutMateriel.EN_SERVICE) svc++;
                else if (st == StatutMateriel.EN_PANNE) pan++;
                else if (st == StatutMateriel.A_CHANGER) chg++;
            }
            out.add(new TypeStat(FAMILLES_LIB[i], total, svc, pan, chg));
        }
        return out;
    }

    private List<TypeStat> parTypeSimple(List<Materiel> liste) {
        List<TypeStat> out = new ArrayList<>();
        for (int i = 0; i < FAMILLES.length; i++) {
            List<TypeMateriel> fam = Arrays.asList(FAMILLES[i]);
            long total = liste.stream().filter(m -> fam.contains(m.getType())).count();
            out.add(new TypeStat(FAMILLES_LIB[i], total, 0, 0, 0));
        }
        return out;
    }

    private List<TypeStat> incidentsParType(List<ReleveMateriel> incidents) {
        List<TypeStat> out = new ArrayList<>();
        for (int i = 0; i < FAMILLES.length; i++) {
            List<TypeMateriel> fam = Arrays.asList(FAMILLES[i]);
            long total = 0, pan = 0, chg = 0;
            for (ReleveMateriel r : incidents) {
                if (r.getMateriel() == null || !fam.contains(r.getMateriel().getType())) continue;
                total++;
                if (r.getStatutObserve() == StatutMateriel.EN_PANNE) pan++;
                else if (r.getStatutObserve() == StatutMateriel.A_CHANGER) chg++;
            }
            out.add(new TypeStat(FAMILLES_LIB[i], total, 0, pan, chg));
        }
        return out;
    }

    // ---------- helpers ----------

    private boolean existeAuFin(Materiel m, int y) {
        Integer ac = anneeCreation(m);
        return ac == null || ac <= y;   // date de création inconnue : supposé pré-existant
    }

    private Integer anneeCreation(Materiel m) {
        return m.getDateCreation() == null ? null
                : LocalDate.ofInstant(m.getDateCreation(), ZoneId.systemDefault()).getYear();
    }

    private int anneeDe(java.time.Instant i) {
        return i == null ? Integer.MIN_VALUE : LocalDate.ofInstant(i, ZoneId.systemDefault()).getYear();
    }

    private int anneeDe(LocalDate d) { return d == null ? Integer.MIN_VALUE : d.getYear(); }

    private boolean estIncident(StatutMateriel s) { return s == StatutMateriel.EN_PANNE || s == StatutMateriel.A_CHANGER; }

    private List<Integer> anneesDisponibles(List<Mission> missions, List<Materiel> materiels, int anneeCourante) {
        int min = anneeCourante;
        for (Mission m : missions) if (m.getDateDebut() != null) min = Math.min(min, m.getDateDebut().getYear());
        for (Materiel m : materiels) { Integer a = anneeCreation(m); if (a != null) min = Math.min(min, a); }
        List<Integer> annees = new ArrayList<>();
        for (int y = anneeCourante; y >= min; y--) annees.add(y);
        return annees;
    }

    private String periode(LocalDate d1, LocalDate d2) {
        if (d1 == null && d2 == null) return "";
        return (d1 == null ? "?" : d1.toString()) + " → " + (d2 == null ? "?" : d2.toString());
    }

    private String etatTemporel(Mission m, int anneeCourante) {
        LocalDate today = LocalDate.now();
        if (m.getDateDebut() != null && m.getDateDebut().isAfter(today)) return "Planifiée";
        if (m.getDateFin() == null || !m.getDateFin().isBefore(today)) return "En cours";
        return "Terminée";
    }

    private String libelleType(Materiel m) {
        return m.getCategorie() != null ? m.getCategorie().getLibelle() : (m.getType() == null ? "" : m.getType().name());
    }

    private String libelleStatut(StatutMateriel s) {
        if (s == null) return "";
        return switch (s) {
            case EN_SERVICE -> "En service";
            case EN_PANNE -> "En panne";
            case A_CHANGER -> "À changer";
        };
    }

    private String nz(String s) { return s == null ? "" : s; }
}
