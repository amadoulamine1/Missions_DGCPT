package sn.dgcpt.missionsparc.agent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private final AgentRepository agentRepo;
    private final PosteRepository posteRepo;

    public AgentService(AgentRepository agentRepo, PosteRepository posteRepo) {
        this.agentRepo = agentRepo;
        this.posteRepo = posteRepo;
    }

    @Transactional(readOnly = true)
    public List<AgentLigne> lister() {
        return agentRepo.findAll().stream()
                .map(a -> new AgentLigne(
                        a.getMatricule(),
                        a.getNom() + " " + a.getPrenom(),
                        a.getFonction(),
                        a.getTypeAgent() == null ? "" : a.getTypeAgent().name(),
                        a.getPoste() == null ? "" : a.getPoste().getNom(),
                        a.getPoste() == null ? null : a.getPoste().getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentForm trouver(String matricule) {
        Agent a = agentRepo.findById(matricule).orElseThrow();
        AgentForm f = new AgentForm();
        f.setMatricule(a.getMatricule());
        f.setNom(a.getNom());
        f.setPrenom(a.getPrenom());
        f.setFonction(a.getFonction());
        f.setTelephone(a.getTelephone());
        f.setEmail(a.getEmail());
        f.setTypeAgent(a.getTypeAgent() == null ? "POSTE" : a.getTypeAgent().name());
        f.setPosteId(a.getPoste() == null ? null : a.getPoste().getId());
        return f;
    }

    @Transactional
    public void creer(AgentForm f) {
        String mat = f.getMatricule() == null ? "" : f.getMatricule().trim();
        if (mat.isEmpty()) throw new IllegalArgumentException("Le matricule est obligatoire.");
        if (agentRepo.existsById(mat)) throw new IllegalArgumentException("Un agent avec le matricule « " + mat + " » existe déjà.");
        Agent a = new Agent();
        a.setMatricule(mat);
        appliquer(a, f);
        agentRepo.save(a);
    }

    @Transactional
    public void modifier(AgentForm f) {
        Agent a = agentRepo.findById(f.getMatricule()).orElseThrow();
        appliquer(a, f);
        agentRepo.save(a);
    }

    private void appliquer(Agent a, AgentForm f) {
        a.setNom(f.getNom());
        a.setPrenom(f.getPrenom());
        a.setFonction(f.getFonction());
        a.setTelephone(f.getTelephone());
        a.setEmail(f.getEmail());
        TypeAgent type = "POSTE".equals(f.getTypeAgent()) ? TypeAgent.POSTE : TypeAgent.INFORMATICIEN;
        a.setTypeAgent(type);
        if (type == TypeAgent.POSTE) {
            if (f.getPosteId() == null) throw new IllegalArgumentException("Un agent de poste doit être rattaché à un TPR.");
            a.setPoste(posteRepo.findById(f.getPosteId()).orElseThrow());
        } else {
            a.setPoste(null);
        }
    }
}
