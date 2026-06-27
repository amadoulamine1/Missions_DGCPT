package sn.dgcpt.missionsparc.mission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.ChefPosteRepository;
import sn.dgcpt.missionsparc.repository.MissionRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Règles métier des missions (§3.4 / §9.2) : au moins un membre, chef de mission désigné
 * parmi les membres, date de fin ≥ date de début, contrôle de chevauchement des membres
 * (excluant la mission éditée), clôture et retrait de membre.
 *
 * {@code ReferentielService} et {@code CanevasWriter} (classes concrètes, non mockables
 * sous JDK récent) ne sont pas sollicités par les chemins testés : un {@code posteId} est
 * fourni, donc la résolution de poste passe par {@code posteRepo} et non par le référentiel.
 */
@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock MissionRepository missionRepo;
    @Mock PosteRepository posteRepo;
    @Mock AgentRepository agentRepo;
    @Mock ChefPosteRepository chefPosteRepo;

    private MissionService service;

    @BeforeEach
    void setUp() {
        service = new MissionService(missionRepo, posteRepo, agentRepo, chefPosteRepo, null, null, null,
                new sn.dgcpt.missionsparc.audit.AuditService(null));
    }

    // ---------- fabriques ----------

    private Agent agent(String mat) {
        Agent a = new Agent(); a.setMatricule(mat); a.setNom("Nom"); a.setPrenom("Prenom");
        a.setTypeAgent(TypeAgent.INFORMATICIEN); return a;
    }

    private Poste poste(int id) { Poste p = new Poste(); p.setId(id); p.setNom("TPR-" + id); return p; }

    private CreationMissionForm formCreation(List<String> membres, String chef) {
        CreationMissionForm f = new CreationMissionForm();
        f.setPosteId(1);
        f.setObjet("Inventaire");
        f.setDateDebut("2026-02-01");
        f.setDateFin("2026-02-10");
        f.setMembres(membres);
        f.setChefMissionSel(chef);
        f.setChefPosteSel("CP1");
        return f;
    }

    // ---------- création ----------

    @Test
    void creer_mission_nominale_genere_reference_et_fige_chef_et_membres() {
        when(posteRepo.findById(1)).thenReturn(Optional.of(poste(1)));
        Agent ag1 = agent("AG1"), ag2 = agent("AG2"), cp1 = agent("CP1");
        when(agentRepo.findById("AG1")).thenReturn(Optional.of(ag1));
        when(agentRepo.findById("AG2")).thenReturn(Optional.of(ag2));
        when(agentRepo.findById("CP1")).thenReturn(Optional.of(cp1));
        when(missionRepo.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        Mission m = service.creer(formCreation(List.of("AG1", "AG2"), "AG1"));

        assertThat(m.getReference()).isEqualTo("MIS-2026-001");
        assertThat(m.getChefMission()).isSameAs(ag1);
        assertThat(m.getChefPosteFige()).isSameAs(cp1);
        assertThat(m.getStatut()).isEqualTo(StatutMission.EN_CONSOLIDATION);
        assertThat(m.getMembres()).extracting(Agent::getMatricule).containsExactlyInAnyOrder("AG1", "AG2");
        verify(missionRepo).save(any(Mission.class));
        verify(chefPosteRepo).save(any(ChefPoste.class)); // historisation du chef de poste
    }

    @Test
    void creer_mission_sans_chef_de_poste_est_autorise() {
        when(posteRepo.findById(1)).thenReturn(Optional.of(poste(1)));
        when(agentRepo.findById("AG1")).thenReturn(Optional.of(agent("AG1")));
        when(missionRepo.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        CreationMissionForm f = formCreation(List.of("AG1"), "AG1");
        f.setChefPosteSel("");   // chef de poste inconnu à la création (renseignable via le canevas)

        Mission m = service.creer(f);

        assertThat(m.getChefPosteFige()).isNull();
        verify(chefPosteRepo, never()).save(any(ChefPoste.class)); // pas d'historisation sans chef
    }

    @Test
    void creer_refuse_une_date_de_fin_anterieure_au_debut() {
        CreationMissionForm f = formCreation(List.of("AG1"), "AG1");
        f.setDateDebut("2026-02-10");
        f.setDateFin("2026-02-01");

        assertThatThrownBy(() -> service.creer(f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date de fin");

        verify(missionRepo, never()).save(any());
    }

    @Test
    void creer_refuse_une_mission_sans_membre() {
        assertThatThrownBy(() -> service.creer(formCreation(List.of(), "AG1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("au moins un membre");
    }

    @Test
    void creer_refuse_un_chef_de_mission_hors_des_membres() {
        assertThatThrownBy(() -> service.creer(formCreation(List.of("AG1"), "AG2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chef de mission parmi les membres");
    }

    @Test
    void creer_autorise_le_chevauchement_de_periode() {
        when(posteRepo.findById(1)).thenReturn(Optional.of(poste(1)));
        when(agentRepo.findById("AG1")).thenReturn(Optional.of(agent("AG1")));
        when(agentRepo.findById("CP1")).thenReturn(Optional.of(agent("CP1")));
        when(missionRepo.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Aucun contrôle de chevauchement : la création réussit même si l'agent est déjà sur une autre mission.
        Mission m = service.creer(formCreation(List.of("AG1"), "AG1"));

        assertThat(m.getReference()).isEqualTo("MIS-2026-001");
        verify(missionRepo).save(any(Mission.class));
    }

    // ---------- modification ----------

    private Mission missionExistante(int id) {
        Mission m = new Mission();
        m.setId(id);
        m.setReference("MIS-2026-00" + id);
        m.setObjet("Objet");
        m.setDateDebut(java.time.LocalDate.of(2026, 2, 1));
        return m;
    }

    private EditionMissionForm formEdition(int id, List<String> membres, String chef) {
        EditionMissionForm f = new EditionMissionForm();
        f.setId(id);
        f.setObjet("Objet modifié");
        f.setDateDebut("2026-02-01");
        f.setDateFin("2026-02-10");
        f.setMembres(membres);
        f.setChefMissionSel(chef);
        return f;
    }

    @Test
    void modifier_met_a_jour_la_mission_et_autorise_le_chevauchement() {
        Mission existante = missionExistante(5);
        Agent ag1 = agent("AG1");
        when(missionRepo.findById(5)).thenReturn(Optional.of(existante));
        when(agentRepo.findById("AG1")).thenReturn(Optional.of(ag1));
        when(missionRepo.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Aucun contrôle de chevauchement : la modification réussit sans vérifier les périodes des autres missions.
        assertThatCode(() -> service.modifier(formEdition(5, List.of("AG1"), "AG1"))).doesNotThrowAnyException();

        verify(missionRepo).save(existante);
        assertThat(existante.getChefMission()).isSameAs(ag1);
    }

    // ---------- clôture et retrait ----------

    @Test
    void cloturer_passe_la_mission_a_l_etat_cloturee() {
        Mission m = missionExistante(7);
        when(missionRepo.findById(7)).thenReturn(Optional.of(m));

        service.cloturer(7);

        assertThat(m.getStatut()).isEqualTo(StatutMission.CLOTUREE);
        verify(missionRepo).save(m);
    }

    @Test
    void cloturer_mission_introuvable_leve_une_exception() {
        when(missionRepo.findById(8)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cloturer(8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void retirerMembre_enleve_l_agent_de_la_mission() {
        Mission m = missionExistante(3);
        m.getMembres().add(agent("AG1"));
        m.getMembres().add(agent("AG2"));
        when(missionRepo.findById(3)).thenReturn(Optional.of(m));

        service.retirerMembre(3, "AG1");

        assertThat(m.getMembres()).extracting(Agent::getMatricule).containsExactly("AG2");
        verify(missionRepo).save(m);
    }

    // ---------- nom de ZIP du canevas ----------

    @Test
    void base_nom_canevas_porte_code_poste_et_periode() {
        Poste p = new Poste(); p.setCode("DKR");
        Mission m = new Mission(); m.setPoste(p);
        m.setDateDebut(java.time.LocalDate.of(2026, 6, 1));
        m.setDateFin(java.time.LocalDate.of(2026, 6, 5));

        assertThat(service.baseNomMission(m)).isEqualTo("Canevas-DKR-2026-06-01_2026-06-05");
    }

    @Test
    void base_nom_canevas_repli_sans_poste_ni_date() {
        assertThat(service.baseNomMission(new Mission())).isEqualTo("Canevas-sans-poste");
    }
}
