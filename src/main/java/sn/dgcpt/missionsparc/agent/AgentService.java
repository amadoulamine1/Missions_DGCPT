package sn.dgcpt.missionsparc.agent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.RattachementAgent;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;
import sn.dgcpt.missionsparc.repository.RattachementAgentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private final AgentRepository agentRepo;
    private final PosteRepository posteRepo;
    private final RattachementAgentRepository rattachementRepo;

    public AgentService(AgentRepository agentRepo, PosteRepository posteRepo,
                        RattachementAgentRepository rattachementRepo) {
        this.agentRepo = agentRepo;
        this.posteRepo = posteRepo;
        this.rattachementRepo = rattachementRepo;
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
                        a.getPoste() == null ? null : a.getPoste().getId(),
                        a.getTelephone(),
                        a.getEmail()))
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
        synchroniserRattachement(a, LocalDate.now());
    }

    @Transactional
    public void modifier(AgentForm f) {
        Agent a = agentRepo.findById(f.getMatricule()).orElseThrow();
        appliquer(a, f);
        agentRepo.save(a);
        synchroniserRattachement(a, LocalDate.now());
    }

    /** Mutation explicite d'un agent de poste vers un autre TPR, à une date d'effet (historisée). */
    @Transactional
    public void muter(String matricule, Integer posteId, LocalDate dateEffet) {
        Agent a = agentRepo.findById(matricule)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable."));
        if (a.getTypeAgent() != TypeAgent.POSTE)
            throw new IllegalArgumentException("Seul un agent de poste peut être muté vers un TPR.");
        if (posteId == null) throw new IllegalArgumentException("Veuillez choisir un poste.");
        Poste nouveau = posteRepo.findById(posteId)
                .orElseThrow(() -> new IllegalArgumentException("Poste introuvable."));
        LocalDate d = (dateEffet == null) ? LocalDate.now() : dateEffet;

        Optional<RattachementAgent> ouvert = rattachementRepo.findFirstByAgent_MatriculeAndDateFinIsNull(matricule);
        if (ouvert.isPresent()) {
            RattachementAgent r = ouvert.get();
            if (r.getPoste() != null && r.getPoste().getId().equals(posteId))
                throw new IllegalArgumentException("L'agent est déjà rattaché à ce poste.");
            if (r.getDateDebut() != null && !d.isAfter(r.getDateDebut()))
                throw new IllegalArgumentException("La date d'effet doit être postérieure au début du rattachement actuel (" + r.getDateDebut() + ").");
            r.setDateFin(d);
            rattachementRepo.saveAndFlush(r);
        }
        RattachementAgent nr = new RattachementAgent();
        nr.setAgent(a);
        nr.setPoste(nouveau);
        nr.setDateDebut(d);
        rattachementRepo.save(nr);
        a.setPoste(nouveau);
        agentRepo.save(a);
    }

    @Transactional(readOnly = true)
    public List<String[]> historiqueRattachement(String matricule) {
        return rattachementRepo.findByAgent_MatriculeOrderByDateDebutDesc(matricule).stream()
                .map(r -> new String[]{
                        r.getPoste() == null ? "" : (r.getPoste().getCode() + " — " + r.getPoste().getNom()),
                        r.getDateDebut() == null ? "" : r.getDateDebut().toString(),
                        r.getDateFin() == null ? "En cours" : r.getDateFin().toString()
                }).toList();
    }

    private void synchroniserRattachement(Agent a, LocalDate dateEffet) {
        Optional<RattachementAgent> ouvert = rattachementRepo.findFirstByAgent_MatriculeAndDateFinIsNull(a.getMatricule());
        Integer posteActuel = (a.getPoste() == null) ? null : a.getPoste().getId();
        Integer posteOuvert = ouvert.map(r -> r.getPoste() == null ? null : r.getPoste().getId()).orElse(null);
        if (Objects.equals(posteActuel, posteOuvert)) return;
        if (ouvert.isPresent()) {
            RattachementAgent r = ouvert.get();
            LocalDate fin = (r.getDateDebut() != null && dateEffet.isBefore(r.getDateDebut())) ? r.getDateDebut() : dateEffet;
            r.setDateFin(fin);
            rattachementRepo.saveAndFlush(r);
        }
        if (a.getPoste() != null) {
            RattachementAgent r = new RattachementAgent();
            r.setAgent(a);
            r.setPoste(a.getPoste());
            r.setDateDebut(dateEffet);
            rattachementRepo.save(r);
        }
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
