package sn.dgcpt.missionsparc.compte;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.AgentRepository;

import java.util.List;

@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final CompteService compte;
    private final AgentRepository agentRepo;

    public UtilisateurController(CompteService compte, AgentRepository agentRepo) {
        this.compte = compte;
        this.agentRepo = agentRepo;
    }

    @ModelAttribute("informaticiens")
    public List<Agent> informaticiens() {
        return agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN);
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("utilisateurs", compte.lister());
        return "utilisateurs";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("form", new UtilisateurForm());
        model.addAttribute("mode", "creation");
        return "utilisateur-form";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Integer id, Model model) {
        model.addAttribute("form", compte.trouver(id));
        model.addAttribute("mode", "modification");
        return "utilisateur-form";
    }

    @PostMapping
    public String creer(@ModelAttribute("form") UtilisateurForm form, Model model) {
        try {
            compte.creer(form);
            return "redirect:/utilisateurs";
        } catch (RuntimeException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("mode", "creation");
            return "utilisateur-form";
        }
    }

    @PostMapping("/{id}")
    public String modifierPost(@PathVariable Integer id, @ModelAttribute("form") UtilisateurForm form, Model model) {
        form.setId(id);
        try {
            compte.modifier(form);
            return "redirect:/utilisateurs";
        } catch (RuntimeException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("mode", "modification");
            return "utilisateur-form";
        }
    }
}
