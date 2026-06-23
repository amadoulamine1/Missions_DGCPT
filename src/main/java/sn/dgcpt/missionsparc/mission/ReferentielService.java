package sn.dgcpt.missionsparc.mission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

/** Résolution (ou création à la volée) des postes et agents référencés. */
@Service
public class ReferentielService {

    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;

    public ReferentielService(PosteRepository posteRepo, AgentRepository agentRepo) {
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
    }

    @Transactional
    public Poste resoudrePoste(String code, String nom) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) throw new IllegalArgumentException("Le code poste est obligatoire.");
        return posteRepo.findByCode(c).orElseGet(() -> {
            Poste p = new Poste();
            p.setCode(c);
            p.setNom((nom == null || nom.isBlank()) ? c : nom.trim());
            return posteRepo.save(p);
        });
    }

    @Transactional
    public Agent resoudreAgent(String matricule, TypeAgent type, Poste poste) {
        if (matricule == null || matricule.isBlank()) return null;
        String mat = matricule.trim();
        return agentRepo.findById(mat).orElseGet(() -> {
            Agent a = new Agent();
            a.setMatricule(mat);
            a.setNom(mat);
            a.setPrenom("-");
            a.setTypeAgent(type);
            a.setPoste(type == TypeAgent.POSTE ? poste : null);
            return agentRepo.save(a);
        });
    }
}
