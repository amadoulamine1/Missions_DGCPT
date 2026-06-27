package sn.dgcpt.missionsparc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.affectation.AffectationService;
import sn.dgcpt.missionsparc.domain.AffectationMateriel;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.Materiel;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.domain.TypeMateriel;
import sn.dgcpt.missionsparc.repository.AffectationMaterielRepository;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.MaterielRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffectationServiceTest {

    @Mock MaterielRepository materielRepo;
    @Mock AgentRepository agentRepo;
    @Mock AffectationMaterielRepository affectationRepo;
    private AffectationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // AuditService concret (non mockable sous JDK 25) : no-op via dépôt null (écriture best-effort).
        service = new AffectationService(materielRepo, agentRepo, affectationRepo,
                new sn.dgcpt.missionsparc.audit.AuditService(null));
    }

    private Poste poste(int id) { Poste p = new Poste(); p.setId(id); p.setNom("P" + id); return p; }

    private Materiel materiel(String num, Poste poste) {
        Materiel m = new Materiel(); m.setNumeroInventaire(num); m.setType(TypeMateriel.ORDINATEUR); m.setPoste(poste); return m;
    }

    private Agent agent(String mat, Poste poste) {
        Agent a = new Agent(); a.setMatricule(mat); a.setNom("Nom"); a.setPrenom("Prenom");
        a.setTypeAgent(TypeAgent.POSTE); a.setPoste(poste); return a;
    }

    @Test
    void reaffecter_refuse_un_agent_d_un_autre_poste() {
        when(materielRepo.findById("ORD-1")).thenReturn(Optional.of(materiel("ORD-1", poste(1))));
        when(agentRepo.findById("A2")).thenReturn(Optional.of(agent("A2", poste(2))));

        assertThatThrownBy(() -> service.reaffecter("ORD-1", "A2", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("même poste");

        verify(affectationRepo, never()).save(any());
    }

    @Test
    void reaffecter_meme_poste_cree_une_nouvelle_affectation() {
        Poste p1 = poste(1);
        Materiel m = materiel("ORD-1", p1);
        when(materielRepo.findById("ORD-1")).thenReturn(Optional.of(m));
        when(agentRepo.findById("A1")).thenReturn(Optional.of(agent("A1", p1)));
        when(affectationRepo.findByMaterielAndDateFinIsNull(m)).thenReturn(Optional.empty());

        assertThatCode(() -> service.reaffecter("ORD-1", "A1", LocalDate.now())).doesNotThrowAnyException();

        verify(affectationRepo, times(1)).save(any(AffectationMateriel.class));
    }
}
