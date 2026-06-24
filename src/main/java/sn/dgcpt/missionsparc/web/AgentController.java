package sn.dgcpt.missionsparc.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sn.dgcpt.missionsparc.agent.AgentForm;
import sn.dgcpt.missionsparc.agent.AgentService;
import sn.dgcpt.missionsparc.repository.PosteRepository;

@Controller
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;
    private final PosteRepository posteRepo;

    public AgentController(AgentService agentService, PosteRepository posteRepo) {
        this.agentService = agentService;
        this.posteRepo = posteRepo;
    }

    @GetMapping
    public String liste(Model model) {
        var all = agentService.lister();
        model.addAttribute("informaticiens", all.stream().filter(a -> "INFORMATICIEN".equals(a.getType())).toList());
        model.addAttribute("agentsPoste", all.stream().filter(a -> "POSTE".equals(a.getType())).toList());
        return "agents";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("form", new AgentForm());
        model.addAttribute("postes", posteRepo.findAll());
        model.addAttribute("mode", "creation");
        return "agent-form";
    }

    @GetMapping("/{matricule}/modifier")
    public String modifier(@PathVariable String matricule, Model model) {
        model.addAttribute("form", agentService.trouver(matricule));
        model.addAttribute("postes", posteRepo.findAll());
        model.addAttribute("mode", "modification");
        return "agent-form";
    }

    @PostMapping
    public String creer(@ModelAttribute("form") AgentForm form, Model model) {
        try {
            agentService.creer(form);
            return "redirect:/agents";
        } catch (RuntimeException e) {
            model.addAttribute("postes", posteRepo.findAll());
            model.addAttribute("mode", "creation");
            model.addAttribute("erreur", e.getMessage());
            return "agent-form";
        }
    }

    @PostMapping("/{matricule}")
    public String mettreAJour(@PathVariable String matricule, @ModelAttribute("form") AgentForm form, Model model) {
        form.setMatricule(matricule);
        try {
            agentService.modifier(form);
            return "redirect:/agents";
        } catch (RuntimeException e) {
            model.addAttribute("postes", posteRepo.findAll());
            model.addAttribute("mode", "modification");
            model.addAttribute("erreur", e.getMessage());
            return "agent-form";
        }
    }
}
