package sn.dgcpt.missionsparc.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.RattachementAgent;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.RattachementAgentRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Historisation du rattachement : ouverture pour un agent de poste créé à la volée, idempotence, hors-périmètre informaticien. */
@ExtendWith(MockitoExtension.class)
class RattachementServiceTest {

    @Mock RattachementAgentRepository repo;
    @InjectMocks RattachementService service;

    @Test
    void ouvre_le_rattachement_d_un_agent_de_poste_nouveau() {
        when(repo.findFirstByAgent_MatriculeAndDateFinIsNull("AGP1")).thenReturn(Optional.empty());

        service.synchroniser(agentPoste("AGP1", poste(1)), LocalDate.of(2026, 6, 1));

        ArgumentCaptor<RattachementAgent> cap = ArgumentCaptor.forClass(RattachementAgent.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getPoste().getId()).isEqualTo(1);
        assertThat(cap.getValue().getDateFin()).as("période ouverte").isNull();
    }

    @Test
    void ignore_un_informaticien() {
        Agent a = new Agent();
        a.setMatricule("IN1");
        a.setTypeAgent(TypeAgent.INFORMATICIEN);

        service.synchroniser(a, LocalDate.now());

        verifyNoInteractions(repo);
    }

    @Test
    void idempotent_si_deja_rattache_au_meme_poste() {
        Poste p = poste(1);
        RattachementAgent ouvert = new RattachementAgent();
        ouvert.setPoste(p);
        ouvert.setDateDebut(LocalDate.of(2026, 1, 1));
        when(repo.findFirstByAgent_MatriculeAndDateFinIsNull("AGP1")).thenReturn(Optional.of(ouvert));

        service.synchroniser(agentPoste("AGP1", p), LocalDate.now());

        verify(repo, never()).save(any());
    }

    private Poste poste(int id) { Poste p = new Poste(); p.setId(id); return p; }

    private Agent agentPoste(String mat, Poste poste) {
        Agent a = new Agent();
        a.setMatricule(mat);
        a.setTypeAgent(TypeAgent.POSTE);
        a.setPoste(poste);
        return a;
    }
}
