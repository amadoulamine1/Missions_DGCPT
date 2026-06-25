package sn.dgcpt.missionsparc.consultation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.consultation.dto.InventaireDateLigne;
import sn.dgcpt.missionsparc.consultation.dto.MaterielVue;
import sn.dgcpt.missionsparc.consultation.dto.MissionVue;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Restitutions et recherches (§6 / §9.10) : filtres du parc (texte, type, statut, poste),
 * état temporel des missions (Planifiée / En cours / Terminée) et inventaire à une date
 * — y compris l'état observé figé (etat_observe, §9.5) au relevé le plus proche.
 */
@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock PosteRepository posteRepo;
    @Mock MaterielRepository materielRepo;
    @Mock ChefPosteRepository chefPosteRepo;
    @Mock AgentRepository agentRepo;
    @Mock MissionRepository missionRepo;
    @Mock ReleveMaterielRepository releveRepo;
    @Mock OrdinateurRepository ordinateurRepo;
    @Mock ImprimanteRepository imprimanteRepo;
    @Mock EquipementReseauRepository reseauRepo;
    @Mock ScannerChequeRepository scannerRepo;
    @Mock AffectationMaterielRepository affectationRepo;
    @InjectMocks ConsultationService service;

    // ---------- fabriques ----------

    private Poste poste(int id, String nom) { Poste p = new Poste(); p.setId(id); p.setNom(nom); return p; }

    private Materiel mat(String num, TypeMateriel type, String nom, String modele, Poste poste, StatutMateriel statut) {
        Materiel m = new Materiel();
        m.setNumeroInventaire(num);
        m.setType(type);
        m.setNom(nom);
        m.setModele(modele);
        m.setPoste(poste);
        m.setStatut(statut);
        return m;
    }

    private Agent agent(String mat, String nom, String prenom) {
        Agent a = new Agent(); a.setMatricule(mat); a.setNom(nom); a.setPrenom(prenom); return a;
    }

    // ---------- filtres du parc ----------

    @Test
    void listerParc_filtre_par_texte_sur_le_nom() {
        Poste p = poste(1, "Dakar");
        when(materielRepo.findAll()).thenReturn(List.of(
                mat("IMP-1", TypeMateriel.IMPRIMANTE, "Imprimante accueil", "HP", p, StatutMateriel.EN_SERVICE),
                mat("SW-1", TypeMateriel.SWITCH, "Switch étage 2", "Cisco", p, StatutMateriel.EN_SERVICE)));

        List<MaterielVue> res = service.listerParc("accueil", null, null, null);

        assertThat(res).extracting(MaterielVue::getNumeroInventaire).containsExactly("IMP-1");
    }

    @Test
    void listerParc_filtre_par_statut() {
        Poste p = poste(1, "Dakar");
        when(materielRepo.findAll()).thenReturn(List.of(
                mat("IMP-1", TypeMateriel.IMPRIMANTE, "Imp A", "HP", p, StatutMateriel.EN_SERVICE),
                mat("IMP-2", TypeMateriel.IMPRIMANTE, "Imp B", "HP", p, StatutMateriel.EN_PANNE)));

        List<MaterielVue> res = service.listerParc(null, null, null, "EN_PANNE");

        assertThat(res).extracting(MaterielVue::getNumeroInventaire).containsExactly("IMP-2");
    }

    @Test
    void listerParc_filtre_par_type_et_par_poste() {
        Poste dkr = poste(1, "Dakar");
        Poste ths = poste(2, "Thiès");
        when(materielRepo.findAll()).thenReturn(List.of(
                mat("IMP-1", TypeMateriel.IMPRIMANTE, "Imp", "HP", dkr, StatutMateriel.EN_SERVICE),
                mat("SW-1", TypeMateriel.SWITCH, "Switch", "Cisco", dkr, StatutMateriel.EN_SERVICE),
                mat("SW-2", TypeMateriel.SWITCH, "Switch", "Cisco", ths, StatutMateriel.EN_SERVICE)));

        assertThat(service.listerParc(null, null, "SWITCH", null))
                .extracting(MaterielVue::getNumeroInventaire).containsExactlyInAnyOrder("SW-1", "SW-2");
        assertThat(service.listerParc(null, 1, null, null))
                .extracting(MaterielVue::getNumeroInventaire).containsExactlyInAnyOrder("IMP-1", "SW-1");
    }

    @Test
    void listerParc_sans_critere_retourne_tout() {
        Poste p = poste(1, "Dakar");
        when(materielRepo.findAll()).thenReturn(List.of(
                mat("IMP-1", TypeMateriel.IMPRIMANTE, "Imp", "HP", p, StatutMateriel.EN_SERVICE),
                mat("SW-1", TypeMateriel.SWITCH, "Switch", "Cisco", p, StatutMateriel.EN_SERVICE)));

        assertThat(service.listerParc(null, null, null, null)).hasSize(2);
    }

    // ---------- état temporel des missions ----------

    private Mission mission(int id, LocalDate debut, LocalDate fin) {
        Mission m = new Mission();
        m.setId(id);
        m.setReference("MIS-" + id);
        m.setObjet("Objet " + id);
        m.setDateDebut(debut);
        m.setDateFin(fin);
        return m;
    }

    @Test
    void listerMissions_derive_l_etat_temporel_des_dates() {
        LocalDate today = LocalDate.now();
        when(missionRepo.findAll()).thenReturn(List.of(
                mission(1, today.plusDays(5), today.plusDays(10)),   // à venir
                mission(2, today.minusDays(2), today.plusDays(2)),   // en cours
                mission(3, today.minusDays(10), today.minusDays(5)))); // terminée

        List<MissionVue> res = service.listerMissions(null, null, null);

        assertThat(res).filteredOn(v -> v.getId() == 1).extracting(MissionVue::getEtat).containsExactly("Planifiée");
        assertThat(res).filteredOn(v -> v.getId() == 2).extracting(MissionVue::getEtat).containsExactly("En cours");
        assertThat(res).filteredOn(v -> v.getId() == 3).extracting(MissionVue::getEtat).containsExactly("Terminée");
    }

    @Test
    void listerMissions_filtre_par_etat_temporel() {
        LocalDate today = LocalDate.now();
        when(missionRepo.findAll()).thenReturn(List.of(
                mission(1, today.plusDays(5), today.plusDays(10)),
                mission(2, today.minusDays(10), today.minusDays(5))));

        List<MissionVue> res = service.listerMissions(null, null, "Terminée");

        assertThat(res).extracting(MissionVue::getId).containsExactly(2);
    }

    // ---------- inventaire à une date (snapshot etat_observe) ----------

    private AffectationMateriel affectation(Materiel m, Agent a, Poste p, LocalDate debut) {
        AffectationMateriel af = new AffectationMateriel();
        af.setMateriel(m); af.setAgent(a); af.setPoste(p); af.setDateDebut(debut);
        return af;
    }

    @Test
    void inventaireALaDate_reporte_l_etat_observe_du_releve_le_plus_proche() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        Poste p = poste(1, "Dakar");
        Materiel m = mat("IMP-1", TypeMateriel.IMPRIMANTE, "Imp", "HP", p, StatutMateriel.EN_PANNE);
        when(affectationRepo.actives(date)).thenReturn(List.of(
                affectation(m, agent("A1", "Diop", "Awa"), p, LocalDate.of(2026, 1, 1))));
        ReleveMateriel releve = new ReleveMateriel();
        releve.setEtatObserve("En panne au relevé de janvier");
        when(releveRepo.findFirstByMateriel_NumeroInventaireAndDateReleveLessThanEqualOrderByDateReleveDesc("IMP-1", date))
                .thenReturn(Optional.of(releve));

        List<InventaireDateLigne> res = service.inventaireALaDate(date);

        assertThat(res).hasSize(1);
        InventaireDateLigne l = res.get(0);
        assertThat(l.getNumeroInventaire()).isEqualTo("IMP-1");
        assertThat(l.getPoste()).isEqualTo("Dakar");
        assertThat(l.getAffecteA()).contains("A1", "Diop", "Awa");
        assertThat(l.getEtatObserve()).isEqualTo("En panne au relevé de janvier");
    }

    @Test
    void inventaireALaDate_etat_observe_vide_si_aucun_releve() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        Poste p = poste(1, "Dakar");
        Materiel m = mat("IMP-1", TypeMateriel.IMPRIMANTE, "Imp", "HP", p, StatutMateriel.EN_SERVICE);
        when(affectationRepo.actives(date)).thenReturn(List.of(
                affectation(m, agent("A1", "Diop", "Awa"), p, LocalDate.of(2026, 1, 1))));
        when(releveRepo.findFirstByMateriel_NumeroInventaireAndDateReleveLessThanEqualOrderByDateReleveDesc("IMP-1", date))
                .thenReturn(Optional.empty());

        List<InventaireDateLigne> res = service.inventaireALaDate(date);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getEtatObserve()).isEmpty();
    }
}
