package sn.dgcpt.missionsparc.importation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.importation.dto.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Intégration en base d'un canevas validé (cf. Specification-import-canevas.md, §6-9).
 * Rapprochement par numéro d'inventaire puis par MAC / numéro de série ;
 * création ou mise à jour du matériel et de son sous-type ; affectations historisées ;
 * relevé daté rattaché à la mission.
 *
 * NB : pour rester testable de bout en bout, les entités référencées absentes
 * (poste, mission, agents, catégorie) sont créées à la volée. En production,
 * leur absence devrait constituer un contrôle bloquant. À défaut, chaque création
 * implicite est <b>journalisée au niveau WARN</b> (préfixe « Import (création à la volée) »)
 * afin qu'un administrateur puisse auditer et valider a posteriori les données introduites.
 */
@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);
    private static final DateTimeFormatter[] FORMATS_DATE = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;
    private final MissionRepository missionRepo;
    private final MaterielRepository materielRepo;
    private final OrdinateurRepository ordinateurRepo;
    private final ImprimanteRepository imprimanteRepo;
    private final EquipementReseauRepository reseauRepo;
    private final ScannerChequeRepository scannerRepo;
    private final AffectationMaterielRepository affectationRepo;
    private final ReleveMaterielRepository releveRepo;
    private final LogicielRepository logicielRepo;
    private final CategorieCableRepository categorieRepo;
    private final CategorieMaterielRepository categorieMaterielRepo;

    public IntegrationService(PosteRepository posteRepo, AgentRepository agentRepo, MissionRepository missionRepo,
                              MaterielRepository materielRepo, OrdinateurRepository ordinateurRepo,
                              ImprimanteRepository imprimanteRepo, EquipementReseauRepository reseauRepo,
                              ScannerChequeRepository scannerRepo, AffectationMaterielRepository affectationRepo,
                              ReleveMaterielRepository releveRepo, LogicielRepository logicielRepo,
                              CategorieCableRepository categorieRepo, CategorieMaterielRepository categorieMaterielRepo) {
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
        this.missionRepo = missionRepo;
        this.materielRepo = materielRepo;
        this.ordinateurRepo = ordinateurRepo;
        this.imprimanteRepo = imprimanteRepo;
        this.reseauRepo = reseauRepo;
        this.scannerRepo = scannerRepo;
        this.affectationRepo = affectationRepo;
        this.releveRepo = releveRepo;
        this.logicielRepo = logicielRepo;
        this.categorieRepo = categorieRepo;
        this.categorieMaterielRepo = categorieMaterielRepo;
    }

    @Transactional
    public int integrer(CanevasImporte canevas) {
        EnteteMission e = canevas.getEntete();
        LocalDate jour = LocalDate.now();

        Poste poste = resoudrePoste(e.getCodePoste(), e.getNomPoste());
        Mission mission = resoudreMission(e, poste, jour);
        if (!vide(e.getObservations())) mission.setObservations(e.getObservations().trim());
        // Relevé réseau : la mission est créée en ligne sans ces champs ; ils sont renseignés dans le
        // canevas par l'agent. On les reporte donc à l'intégration, même si la mission existe déjà.
        if (!vide(e.getEtatCablage())) mission.setEtatCablage(e.getEtatCablage().trim());
        if (!vide(e.getCategorieCable())) mission.setCategorieCable(resoudreCategorie(e.getCategorieCable()));
        // Chef de poste éventuellement inconnu à la création de la mission : renseigné ici depuis le
        // canevas s'il n'a pas encore été fixé.
        if (mission.getChefPosteFige() == null && !vide(e.getChefPoste()))
            mission.setChefPosteFige(resoudreAgent(e.getChefPoste(), TypeAgent.POSTE, poste));
        Agent saisisseur = resoudreAgent(e.getAgentSaisisseur(), TypeAgent.INFORMATICIEN, null);
        String zone = vide(e.getZone()) ? null : e.getZone();

        // Agents de la TPR déclarés dans le canevas : créés/complétés en base (attributaires éventuellement nouveaux)
        for (LigneAgentPoste a : canevas.getAgentsTpr()) { upsertAgentPoste(a, poste); }

        // Membres du canevas fusionnés dans la mission
        for (LigneMembre m : canevas.getMembres()) {
            Agent a = resoudreAgent(m.getMatricule(), TypeAgent.INFORMATICIEN, null);
            if (a != null) mission.getMembres().add(a);
        }

        // Règle : l'agent saisisseur doit être un membre de la mission
        boolean estMembre = false;
        if (saisisseur != null) {
            for (Agent a : mission.getMembres()) {
                if (a.getMatricule().equals(saisisseur.getMatricule())) { estMembre = true; break; }
            }
        }
        if (!estMembre) {
            throw new IllegalStateException("L'agent saisisseur (" + e.getAgentSaisisseur()
                    + ") doit être un membre de la mission " + mission.getReference() + ".");
        }

        int nb = 0;
        for (LigneOrdinateur o : canevas.getOrdinateurs()) { integrerOrdinateur(o, poste, mission, saisisseur, zone, jour); nb++; }
        for (LigneImprimante i : canevas.getImprimantes()) { integrerImprimante(i, poste, mission, saisisseur, zone, jour); nb++; }
        for (LigneEquipementReseau eq : canevas.getEquipementsReseau()) { integrerReseau(eq, poste, mission, saisisseur, zone, jour); nb++; }
        for (LigneScanner s : canevas.getScanners()) { integrerScanner(s, poste, mission, saisisseur, zone, jour); nb++; }
        for (LigneAutreMateriel a : canevas.getAutres()) { integrerAutre(a, poste, mission, saisisseur, zone, jour); nb++; }

        missionRepo.save(mission);

        log.info("Intégration mission={} : {} matériel(s), {} membre(s)",
                mission.getReference(), nb, canevas.getMembres().size());
        return nb;
    }

    // ---------- résolutions ----------

    private Poste resoudrePoste(String code, String nom) {
        String c = trim(code);
        if (c.isEmpty()) throw new IllegalStateException("Code poste manquant dans l'en-tête.");
        return posteRepo.findByCode(c).orElseGet(() -> {
            Poste p = new Poste();
            p.setCode(c);
            p.setNom(vide(nom) ? c : nom.trim());
            log.warn("Import (création à la volée) : poste inconnu « {} » (nom={}) créé — à valider par un administrateur.", c, p.getNom());
            return posteRepo.save(p);
        });
    }

    private Mission resoudreMission(EnteteMission e, Poste poste, LocalDate jour) {
        String ref = trim(e.getReference());
        return missionRepo.findByReference(ref).orElseGet(() -> {
            Mission m = new Mission();
            m.setReference(ref);
            m.setObjet(vide(e.getObjet()) ? "(import)" : e.getObjet());
            m.setDateDebut(parseDate(e.getDateDebut(), jour));
            m.setDateFin(parseDate(e.getDateFin(), null));
            m.setPoste(poste);
            m.setChefMission(resoudreAgent(e.getChefMission(), TypeAgent.INFORMATICIEN, null));
            m.setChefPosteFige(resoudreAgent(e.getChefPoste(), TypeAgent.POSTE, poste));
            m.setEtatCablage(vide(e.getEtatCablage()) ? null : e.getEtatCablage());
            m.setCategorieCable(resoudreCategorie(e.getCategorieCable()));
            m.setStatut(StatutMission.EN_CONSOLIDATION);
            return missionRepo.save(m);
        });
    }

    private Agent resoudreAgent(String matricule, TypeAgent type, Poste poste) {
        if (vide(matricule)) return null;
        String mat = matricule.trim();
        return agentRepo.findById(mat).orElseGet(() -> {
            Agent a = new Agent();
            a.setMatricule(mat);
            a.setNom(mat);
            a.setPrenom("-");
            a.setTypeAgent(type);
            a.setPoste(type == TypeAgent.POSTE ? poste : null);
            log.warn("Import (création à la volée) : agent {} inconnu « {} » créé (nom/prénom à compléter) — à valider par un administrateur.", type, mat);
            return agentRepo.save(a);
        });
    }

    /** Crée ou complète un agent de poste déclaré dans la feuille « Agents TPR » du canevas. */
    private void upsertAgentPoste(LigneAgentPoste l, Poste poste) {
        String mat = trim(l.getMatricule());
        if (mat.isEmpty()) return;
        Agent a = agentRepo.findById(mat).orElse(null);
        boolean nouveau = (a == null);
        if (nouveau) {
            a = new Agent();
            a.setMatricule(mat);
            a.setTypeAgent(TypeAgent.POSTE);
            a.setPoste(poste);
            log.warn("Import (création à la volée) : agent de poste « {} » créé pour le poste {} — à valider par un administrateur.",
                    mat, poste == null ? "?" : poste.getCode());
        }
        if (!vide(l.getNom())) a.setNom(l.getNom().trim());
        else if (nouveau) a.setNom(mat);
        if (!vide(l.getPrenom())) a.setPrenom(l.getPrenom().trim());
        else if (nouveau) a.setPrenom("-");
        if (!vide(l.getFonction())) a.setFonction(l.getFonction().trim());
        if (!vide(l.getTelephone())) a.setTelephone(l.getTelephone().trim());
        if (!vide(l.getEmail())) a.setEmail(l.getEmail().trim());
        agentRepo.save(a);
    }

    private StatutMateriel parseStatut(String v) {
        if (vide(v)) return null;
        String x = v.trim().toLowerCase();
        if (x.startsWith("en panne")) return StatutMateriel.EN_PANNE;
        if (x.startsWith("à changer") || x.startsWith("a changer")) return StatutMateriel.A_CHANGER;
        if (x.startsWith("en service")) return StatutMateriel.EN_SERVICE;
        return null;
    }

    private CategorieCable resoudreCategorie(String libelle) {
        if (vide(libelle)) return null;
        String lib = libelle.trim();
        return categorieRepo.findByLibelle(lib).orElseGet(() -> {
            CategorieCable c = new CategorieCable();
            c.setLibelle(lib);
            log.warn("Import (création à la volée) : catégorie de câble inconnue « {} » créée — à valider par un administrateur.", lib);
            return categorieRepo.save(c);
        });
    }

    /** Catégorie système (seedée) d'une famille technique : sert de type affiché aux onglets câblés. */
    private CategorieMateriel categorieSysteme(TypeMateriel famille) {
        return categorieMaterielRepo.findFirstByFamilleAndSystemeTrue(famille)
                .orElseThrow(() -> new IllegalStateException("Catégorie système manquante pour la famille " + famille + "."));
    }

    // ---------- intégration par type ----------

    private void integrerOrdinateur(LigneOrdinateur o, Poste poste, Mission mission, Agent saisisseur, String zone, LocalDate jour) {
        Materiel mat = rapprocher(o.getNumeroInventaire(), categorieSysteme(TypeMateriel.ORDINATEUR), o.getNomMachine(), o.getModele(), poste, o.getStatut(), o.getObservation(),
                () -> vide(o.getMacEthernet()) ? Optional.empty()
                        : ordinateurRepo.findByMacEthernet(o.getMacEthernet()).map(Ordinateur::getMateriel));
        Ordinateur ord = ordinateurRepo.findById(mat.getNumeroInventaire()).orElseGet(Ordinateur::new);
        ord.setMateriel(mat);
        ord.setMacEthernet(nullSiVide(o.getMacEthernet()));
        ord.setMacWifi(nullSiVide(o.getMacWifi()));
        ord.setNomMachine(o.getNomMachine());
        ord.setRam(nullSiVide(o.getRam()));
        ord.setProcesseur(nullSiVide(o.getProcesseur()));
        ord.setDisqueDur(nullSiVide(o.getDisqueDur()));
        Agent traitant = resoudreAgent(o.getAgentInstallateur(), TypeAgent.INFORMATICIEN, null);
        ord.setAgentInstallateur(traitant); // « dernier agent traitant » conservé sur la machine
        ord.setLogiciels(logicielsDe(o));
        ordinateurRepo.save(ord);

        Agent attributaire = resoudreAgent(o.getAgentAttributaire(), TypeAgent.POSTE, poste);
        majAffectation(mat, attributaire, poste, jour);
        majReleve(mission, mat, saisisseur, traitant, zone, jour); // agent traitant historisé sur le relevé
    }

    private void integrerImprimante(LigneImprimante i, Poste poste, Mission mission, Agent saisisseur, String zone, LocalDate jour) {
        Materiel mat = rapprocher(i.getNumeroInventaire(), categorieSysteme(TypeMateriel.IMPRIMANTE), i.getNom(), i.getModele(), poste, i.getStatut(), i.getObservation(),
                () -> vide(i.getMac()) ? Optional.empty()
                        : imprimanteRepo.findByMac(i.getMac()).map(Imprimante::getMateriel));
        Imprimante imp = imprimanteRepo.findById(mat.getNumeroInventaire()).orElseGet(Imprimante::new);
        imp.setMateriel(mat);
        imp.setMac(nullSiVide(i.getMac()));
        imp.setMacWifi(nullSiVide(i.getMacWifi()));
        imp.setIp(nullSiVide(i.getIp()));
        imprimanteRepo.save(imp);
        majAffectation(mat, null, poste, jour);
        majReleve(mission, mat, saisisseur, null, zone, jour);
    }

    private void integrerReseau(LigneEquipementReseau eq, Poste poste, Mission mission, Agent saisisseur, String zone, LocalDate jour) {
        TypeMateriel type = "Access point".equalsIgnoreCase(trim(eq.getType())) ? TypeMateriel.ACCESS_POINT : TypeMateriel.SWITCH;
        Materiel mat = rapprocher(eq.getNumeroInventaire(), categorieSysteme(type), eq.getNom(), eq.getModele(), poste, eq.getStatut(), eq.getObservation(),
                () -> vide(eq.getMac()) ? Optional.empty()
                        : reseauRepo.findByMac(eq.getMac()).map(EquipementReseau::getMateriel));
        EquipementReseau er = reseauRepo.findById(mat.getNumeroInventaire()).orElseGet(EquipementReseau::new);
        er.setMateriel(mat);
        er.setMac(nullSiVide(eq.getMac()));
        er.setIp(nullSiVide(eq.getIp()));
        reseauRepo.save(er);
        majAffectation(mat, null, poste, jour);
        majReleve(mission, mat, saisisseur, null, zone, jour);
    }

    private void integrerScanner(LigneScanner s, Poste poste, Mission mission, Agent saisisseur, String zone, LocalDate jour) {
        Materiel mat = rapprocher(s.getNumeroInventaire(), categorieSysteme(TypeMateriel.SCANNER_CHEQUE), null, s.getModele(), poste, s.getStatut(), s.getObservation(),
                () -> vide(s.getNumeroSerie()) ? Optional.empty()
                        : scannerRepo.findByNumeroSerie(s.getNumeroSerie()).map(ScannerCheque::getMateriel));
        ScannerCheque sc = scannerRepo.findById(mat.getNumeroInventaire()).orElseGet(ScannerCheque::new);
        sc.setMateriel(mat);
        sc.setNumeroSerie(nullSiVide(s.getNumeroSerie()));
        sc.setMarque(nullSiVide(s.getMarque()));
        scannerRepo.save(sc);
        majAffectation(mat, null, poste, jour);
        majReleve(mission, mat, saisisseur, null, zone, jour);
    }

    /** Matériel d'un type paramétrable (famille AUTRE) : attributs communs uniquement. */
    private void integrerAutre(LigneAutreMateriel a, Poste poste, Mission mission, Agent saisisseur, String zone, LocalDate jour) {
        CategorieMateriel categorie = vide(a.getTypeLibelle())
                ? categorieSysteme(TypeMateriel.AUTRE)
                : categorieMaterielRepo.findByLibelleIgnoreCase(a.getTypeLibelle().trim())
                        .orElseGet(() -> categorieSysteme(TypeMateriel.AUTRE));
        Materiel mat = rapprocher(a.getNumeroInventaire(), categorie, a.getNom(), a.getModele(), poste, a.getStatut(), a.getObservation(),
                () -> vide(a.getMac()) ? Optional.empty() : materielRepo.findByMac(a.getMac().trim()));
        if (!vide(a.getMac())) mat.setMac(a.getMac().trim());
        if (!vide(a.getIp())) mat.setIp(a.getIp().trim());
        materielRepo.save(mat);
        majAffectation(mat, null, poste, jour);
        majReleve(mission, mat, saisisseur, null, zone, jour);
    }

    // ---------- noyau ----------

    private Materiel rapprocher(String numero, CategorieMateriel categorie, String nom, String modele, Poste poste,
                                String statut, String observation,
                                Supplier<Optional<Materiel>> parCleNaturelle) {
        Materiel mat;
        if (!vide(numero)) {
            mat = materielRepo.findById(numero.trim()).orElse(null);
            if (mat == null) {
                mat = new Materiel();
                mat.setNumeroInventaire(numero.trim());
                mat.setDateCreation(Instant.now());
            }
        } else {
            mat = parCleNaturelle.get().orElse(null);
            if (mat == null) {
                mat = new Materiel();
                mat.setNumeroInventaire(genererNumero(poste, categorie));
                mat.setDateCreation(Instant.now());
            }
        }
        mat.setCategorie(categorie);
        mat.setType(categorie.getFamille());
        if (!vide(nom)) mat.setNom(nom);
        if (!vide(modele)) mat.setModele(modele);
        mat.setPoste(poste);
        StatutMateriel st = parseStatut(statut);
        if (st != null) mat.setStatut(st);
        else if (mat.getStatut() == null) mat.setStatut(StatutMateriel.EN_SERVICE);
        if (!vide(observation)) mat.setObservation(observation.trim());
        return materielRepo.save(mat);
    }

    private String genererNumero(Poste poste, CategorieMateriel categorie) {
        String prefixe = poste.getCode() + "-" + categorie.getPrefixe() + "-";
        long n = materielRepo.countByNumeroInventaireStartingWith(prefixe) + 1;
        return prefixe + String.format("%04d", n);
    }

    private void majAffectation(Materiel mat, Agent agent, Poste poste, LocalDate jour) {
        String matriculeNouveau = (agent == null) ? null : agent.getMatricule();
        Optional<AffectationMateriel> courante = affectationRepo.findByMaterielAndDateFinIsNull(mat);
        if (courante.isPresent()) {
            AffectationMateriel a = courante.get();
            String matriculeActuel = (a.getAgent() == null) ? null : a.getAgent().getMatricule();
            Integer posteActuel = (a.getPoste() == null) ? null : a.getPoste().getId();
            if (Objects.equals(matriculeActuel, matriculeNouveau) && Objects.equals(posteActuel, poste.getId())) {
                return; // affectation inchangée
            }
            a.setDateFin(jour);
            affectationRepo.saveAndFlush(a); // libère l'unicité "une seule affectation courante"
        }
        AffectationMateriel nouvelle = new AffectationMateriel();
        nouvelle.setMateriel(mat);
        nouvelle.setAgent(agent);
        nouvelle.setPoste(poste);
        nouvelle.setDateDebut(jour);
        affectationRepo.save(nouvelle);
    }

    private void majReleve(Mission mission, Materiel mat, Agent saisisseur, Agent traitant, String zone, LocalDate jour) {
        ReleveMateriel r = releveRepo.findByMissionAndMateriel(mission, mat).orElseGet(ReleveMateriel::new);
        r.setMission(mission);
        r.setMateriel(mat);
        r.setAgentSaisisseur(saisisseur);
        r.setAgentTraitant(traitant);
        r.setZone(zone);
        r.setDateReleve(jour);
        r.setStatutObserve(mat.getStatut()); // statut observé à cette mission (historique)
        r.setEtatObserve(construireEtatObserve(mat));
        releveRepo.save(r);
    }

    /** Photo datée : résume les attributs observés du matériel au moment du relevé. */
    private String construireEtatObserve(Materiel mat) {
        StringBuilder sb = new StringBuilder();
        ajoutEtat(sb, "Nom", mat.getNom());
        ajoutEtat(sb, "Modèle", mat.getModele());
        ajoutEtat(sb, "Statut", mat.getStatut() == null ? null : mat.getStatut().name());
        switch (mat.getType()) {
            case ORDINATEUR -> ordinateurRepo.findById(mat.getNumeroInventaire()).ifPresent(o -> {
                ajoutEtat(sb, "MAC eth", o.getMacEthernet());
                ajoutEtat(sb, "MAC wifi", o.getMacWifi());
                ajoutEtat(sb, "RAM", o.getRam());
                ajoutEtat(sb, "Processeur", o.getProcesseur());
                ajoutEtat(sb, "Disque", o.getDisqueDur());
                ajoutEtat(sb, "Logiciels", o.getLogiciels().stream().map(Logiciel::getNom).sorted().collect(java.util.stream.Collectors.joining(", ")));
            });
            case IMPRIMANTE -> imprimanteRepo.findById(mat.getNumeroInventaire()).ifPresent(i -> {
                ajoutEtat(sb, "MAC", i.getMac());
                ajoutEtat(sb, "IP", i.getIp());
            });
            case SWITCH, ACCESS_POINT -> reseauRepo.findById(mat.getNumeroInventaire()).ifPresent(e -> {
                ajoutEtat(sb, "MAC", e.getMac());
                ajoutEtat(sb, "IP", e.getIp());
            });
            case SCANNER_CHEQUE -> scannerRepo.findById(mat.getNumeroInventaire()).ifPresent(sc -> {
                ajoutEtat(sb, "N° série", sc.getNumeroSerie());
                ajoutEtat(sb, "Marque", sc.getMarque());
            });
            default -> { }
        }
        return sb.toString();
    }

    private void ajoutEtat(StringBuilder sb, String cle, String val) {
        if (val != null && !val.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(cle).append(": ").append(val.trim());
        }
    }

    private Set<Logiciel> logicielsDe(LigneOrdinateur o) {
        Set<Logiciel> set = new HashSet<>();
        ajouterLogiciel(set, o.isAster(), "Aster");
        ajouterLogiciel(set, o.isAntivirus(), "Antivirus");
        ajouterLogiciel(set, o.isSicCDD(), "SicCDD");
        ajouterLogiciel(set, o.isCic(), "CIC");
        ajouterLogiciel(set, o.isSysbudget(), "Sysbudget");
        ajouterLogiciel(set, o.isAd(), "AD");
        return set;
    }

    private void ajouterLogiciel(Set<Logiciel> set, boolean present, String nom) {
        if (present) logicielRepo.findByNom(nom).ifPresent(set::add);
    }

    // ---------- utilitaires ----------

    private LocalDate parseDate(String s, LocalDate defaut) {
        if (vide(s)) return defaut;
        String v = s.trim();
        for (DateTimeFormatter f : FORMATS_DATE) {
            try { return LocalDate.parse(v, f); } catch (DateTimeParseException ignored) { }
        }
        return defaut;
    }

    private boolean vide(String s) { return s == null || s.isBlank(); }
    private String trim(String s) { return s == null ? "" : s.trim(); }
    private String nullSiVide(String s) { return vide(s) ? null : s.trim(); }
}
