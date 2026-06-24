package sn.dgcpt.missionsparc.affectation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.AffectationMateriel;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.Materiel;
import sn.dgcpt.missionsparc.repository.AffectationMaterielRepository;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.MaterielRepository;

import java.time.LocalDate;
import java.util.Optional;

/** Réaffectation d'un matériel à un agent, en conservant l'historique (affectations datées). */
@Service
public class AffectationService {

    private final MaterielRepository materielRepo;
    private final AgentRepository agentRepo;
    private final AffectationMaterielRepository affectationRepo;

    public AffectationService(MaterielRepository materielRepo, AgentRepository agentRepo,
                              AffectationMaterielRepository affectationRepo) {
        this.materielRepo = materielRepo;
        this.agentRepo = agentRepo;
        this.affectationRepo = affectationRepo;
    }

    @Transactional
    public void reaffecter(String numero, String agentMatricule, LocalDate dateEffet) {
        Materiel m = materielRepo.findById(numero)
                .orElseThrow(() -> new IllegalArgumentException("Matériel introuvable."));
        if (agentMatricule == null || agentMatricule.isBlank())
            throw new IllegalArgumentException("Veuillez choisir un agent.");
        Agent agent = agentRepo.findById(agentMatricule)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable."));

        // La réaffectation est limitée à un agent rattaché au même poste (TPR) que le matériel.
        Integer posteMateriel = (m.getPoste() == null) ? null : m.getPoste().getId();
        Integer posteAgent = (agent.getPoste() == null) ? null : agent.getPoste().getId();
        if (posteMateriel == null)
            throw new IllegalArgumentException("Ce matériel n'est rattaché à aucun poste.");
        if (posteAgent == null || !posteAgent.equals(posteMateriel))
            throw new IllegalArgumentException("L'agent choisi doit être rattaché au même poste (TPR) que le matériel.");

        LocalDate d = (dateEffet == null) ? LocalDate.now() : dateEffet;

        Optional<AffectationMateriel> courante = affectationRepo.findByMaterielAndDateFinIsNull(m);
        if (courante.isPresent()) {
            AffectationMateriel a = courante.get();
            if (a.getAgent() != null && agent.getMatricule().equals(a.getAgent().getMatricule()))
                throw new IllegalArgumentException("Ce matériel est déjà affecté à cet agent.");
            if (a.getDateDebut() != null && !d.isAfter(a.getDateDebut()))
                throw new IllegalArgumentException("La date d'effet doit être postérieure au début de l'affectation actuelle (" + a.getDateDebut() + ").");
            a.setDateFin(d);
            affectationRepo.save(a);
        }
        AffectationMateriel nouvelle = new AffectationMateriel();
        nouvelle.setMateriel(m);
        nouvelle.setAgent(agent);
        nouvelle.setPoste(m.getPoste());
        nouvelle.setDateDebut(d);
        nouvelle.setDateFin(null);
        affectationRepo.save(nouvelle);
    }
}
