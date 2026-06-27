package sn.dgcpt.missionsparc.consultation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.consultation.dto.RapportAnnuelVue;
import sn.dgcpt.missionsparc.consultation.dto.SerieAnnuelle;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Vérifie l'agrégation du rapport annuel : filtrage par année, statut du parc « au 31/12 » via le
 * dernier relevé daté, et calcul des écarts N vs N‑1. Dépôts mockés (interfaces), aucune base requise.
 */
@ExtendWith(MockitoExtension.class)
class RapportAnnuelServiceTest {

    @Mock MaterielRepository materielRepo;
    @Mock MissionRepository missionRepo;
    @Mock ReleveMaterielRepository releveRepo;
    @Mock AffectationMaterielRepository affectationRepo;

    private Materiel materiel(String num, TypeMateriel type, StatutMateriel st, Poste p, int annee) {
        Materiel m = new Materiel();
        m.setNumeroInventaire(num); m.setType(type); m.setStatut(st); m.setNom(num); m.setPoste(p);
        m.setDateCreation(Instant.parse(annee + "-06-15T10:00:00Z"));
        return m;
    }

    private Mission mission(int id, String ref, LocalDate debut, Poste p, Agent chef) {
        Mission m = new Mission();
        m.setId(id); m.setReference(ref); m.setObjet("Objet"); m.setDateDebut(debut); m.setDateFin(debut.plusDays(4));
        m.setPoste(p); m.setChefMission(chef); m.getMembres().add(chef);
        return m;
    }

    private ReleveMateriel releve(Materiel m, Mission mis, LocalDate d, StatutMateriel obs) {
        ReleveMateriel r = new ReleveMateriel();
        r.setMateriel(m); r.setMission(mis); r.setDateReleve(d); r.setStatutObserve(obs);
        return r;
    }

    private SerieAnnuelle serie(RapportAnnuelVue v, String prefixe) {
        return v.getSeries().stream().filter(s -> s.getLibelle().startsWith(prefixe)).findFirst().orElseThrow();
    }

    @Test
    void agrege_l_annee_le_parc_au_31_12_et_les_ecarts() {
        Poste p = new Poste(); p.setId(1); p.setNom("Dakar");
        Agent chef = new Agent(); chef.setMatricule("IN001"); chef.setNom("Diop"); chef.setPrenom("Awa");
        chef.setTypeAgent(TypeAgent.INFORMATICIEN);

        Materiel m1 = materiel("PC-1", TypeMateriel.ORDINATEUR, StatutMateriel.EN_SERVICE, p, 2025);
        Materiel m2 = materiel("PC-2", TypeMateriel.ORDINATEUR, StatutMateriel.EN_PANNE, p, 2026);

        Mission mis2025 = mission(1, "MIS-2025-001", LocalDate.of(2025, 3, 1), p, chef);
        Mission mis2026 = mission(2, "MIS-2026-001", LocalDate.of(2026, 4, 1), p, chef);

        ReleveMateriel r1 = releve(m1, mis2026, LocalDate.of(2026, 4, 2), StatutMateriel.EN_SERVICE);
        ReleveMateriel r2 = releve(m2, mis2026, LocalDate.of(2026, 4, 2), StatutMateriel.EN_PANNE);

        when(materielRepo.findAll()).thenReturn(List.of(m1, m2));
        when(missionRepo.findAll()).thenReturn(List.of(mis2025, mis2026));
        when(releveRepo.findAll()).thenReturn(List.of(r1, r2));
        when(affectationRepo.findAll()).thenReturn(List.of());

        RapportAnnuelService service = new RapportAnnuelService(materielRepo, missionRepo, releveRepo, affectationRepo);
        RapportAnnuelVue v = service.rapport(2026, 2);

        // Missions de l'année 2026 : une seule
        assertThat(v.getMissionsAnnee()).hasSize(1);
        assertThat(v.getMissionsAnnee().get(0)[0]).isEqualTo("MIS-2026-001");

        // Parc au 31/12/2026 : 2 équipements, statut pris du dernier relevé ≤ 31/12
        assertThat(v.getParcTaille()).isEqualTo(2);
        assertThat(v.getParcSvc()).isEqualTo(1);
        assertThat(v.getParcPan()).isEqualTo(1);
        assertThat(v.getParcDispo()).isEqualTo(50);

        // Incidents 2026 : 1 (la panne)
        assertThat(v.getIncidentsListe()).hasSize(1);
        assertThat(serie(v, "Incidents").getValeurN()).isEqualTo(1);

        // Taille du parc : 1 fin 2025 (m2 créé en 2026) → 2 fin 2026, écart +1
        SerieAnnuelle taille = serie(v, "Taille du parc");
        assertThat(taille.getValeurN()).isEqualTo(2);
        assertThat(taille.getValeurN1()).isEqualTo(1);
        assertThat(taille.getDelta()).isEqualTo(1);

        // Activité agents : le chef est membre de la mission 2026
        assertThat(v.getActiviteAgents()).anySatisfy(a -> assertThat(a[0]).isEqualTo("IN001"));
    }
}
