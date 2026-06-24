package sn.dgcpt.missionsparc.mission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.ChefPoste;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.ChefPosteRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Désignation / changement du chef de poste d'un TPR, avec historisation des périodes. */
@Service
public class ChefPosteService {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;
    private final ChefPosteRepository chefPosteRepo;

    public ChefPosteService(PosteRepository posteRepo, AgentRepository agentRepo, ChefPosteRepository chefPosteRepo) {
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
        this.chefPosteRepo = chefPosteRepo;
    }

    @Transactional
    public void designer(Integer posteId, DesignationChefForm f) {
        Poste poste = posteRepo.findById(posteId).orElseThrow();
        LocalDate dateEffet = parseDate(f.getDateEffet(), LocalDate.now());
        Agent chef = resoudreAgent(f, poste);

        Optional<ChefPoste> courant = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(posteId);
        if (courant.isPresent()) {
            ChefPoste c = courant.get();
            if (c.getAgent().getMatricule().equals(chef.getMatricule())) {
                return; // déjà chef de ce poste : rien à changer
            }
            if (dateEffet.isBefore(c.getDateDebut())) {
                throw new IllegalArgumentException(
                        "La date d'effet doit être postérieure au début du mandat du chef actuel (" + c.getDateDebut() + ").");
            }
            c.setDateFin(dateEffet);
            chefPosteRepo.saveAndFlush(c);
        }
        ChefPoste cp = new ChefPoste();
        cp.setPoste(poste);
        cp.setAgent(chef);
        cp.setDateDebut(dateEffet);
        chefPosteRepo.save(cp);
    }

    private Agent resoudreAgent(DesignationChefForm f, Poste poste) {
        if (f.getChefSel() != null && !f.getChefSel().isBlank()) {
            return agentRepo.findById(f.getChefSel().trim()).orElseThrow();
        }
        final String mat = (f.getChefMat() == null) ? "" : f.getChefMat().trim();
        if (mat.isEmpty()) {
            throw new IllegalArgumentException("Choisissez un agent existant ou renseignez le matricule du nouveau chef.");
        }
        return agentRepo.findById(mat).orElseGet(() -> {
            Agent a = new Agent();
            a.setMatricule(mat);
            a.setNom((f.getChefNom() == null || f.getChefNom().isBlank()) ? mat : f.getChefNom().trim());
            a.setPrenom((f.getChefPrenom() == null || f.getChefPrenom().isBlank()) ? "-" : f.getChefPrenom().trim());
            a.setTypeAgent(TypeAgent.POSTE);
            a.setPoste(poste);
            return agentRepo.save(a);
        });
    }

    private LocalDate parseDate(String s, LocalDate defaut) {
        if (s == null || s.isBlank()) return defaut;
        String v = s.trim();
        for (DateTimeFormatter f : FORMATS) {
            try { return LocalDate.parse(v, f); } catch (DateTimeParseException ignored) { }
        }
        return defaut;
    }
}
