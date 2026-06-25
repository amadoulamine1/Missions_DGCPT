package sn.dgcpt.missionsparc.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.mission.ChefPosteService;
import sn.dgcpt.missionsparc.mission.DesignationChefForm;
import sn.dgcpt.missionsparc.mission.PosteForm;
import sn.dgcpt.missionsparc.repository.PosteRepository;

@Controller
@RequestMapping("/postes")
public class PosteController {

    private final PosteRepository posteRepo;
    private final ChefPosteService chefPosteService;

    public PosteController(PosteRepository posteRepo, ChefPosteService chefPosteService) {
        this.posteRepo = posteRepo;
        this.chefPosteService = chefPosteService;
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("form", new PosteForm());
        model.addAttribute("mode", "creation");
        return "poste-form";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Integer id, Model model) {
        Poste p = posteRepo.findById(id).orElseThrow();
        PosteForm f = new PosteForm();
        f.setId(p.getId());
        f.setCode(p.getCode());
        f.setNom(p.getNom());
        f.setRegion(p.getRegion());
        model.addAttribute("form", f);
        model.addAttribute("mode", "modification");
        return "poste-form";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("form") PosteForm form, BindingResult br, Model model) {
        String code = form.getCode() == null ? "" : form.getCode().trim();
        if (!br.hasFieldErrors("code") && posteRepo.findByCode(code).isPresent())
            br.rejectValue("code", "duplicate", "Un poste avec le code « " + code + " » existe déjà.");
        if (br.hasErrors()) {
            model.addAttribute("mode", "creation");
            return "poste-form";
        }
        Poste p = new Poste();
        p.setCode(code);
        p.setNom(form.getNom());
        p.setRegion(form.getRegion());
        posteRepo.save(p);
        return "redirect:/postes";
    }

    @PostMapping("/{id}")
    public String mettreAJour(@PathVariable Integer id, @Valid @ModelAttribute("form") PosteForm form,
                              BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("mode", "modification");
            return "poste-form";
        }
        Poste p = posteRepo.findById(id).orElseThrow();
        p.setCode(form.getCode() == null ? p.getCode() : form.getCode().trim());
        p.setNom(form.getNom());
        p.setRegion(form.getRegion());
        posteRepo.save(p);
        return "redirect:/postes";
    }

    @PostMapping("/{id}/chef")
    public String designerChef(@PathVariable Integer id, @ModelAttribute DesignationChefForm form, RedirectAttributes ra) {
        try {
            chefPosteService.designer(id, form);
            ra.addFlashAttribute("message", "Chef de poste mis à jour.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage() != null ? e.getMessage() : "Désignation impossible.");
        }
        return "redirect:/postes/" + id;
    }
}
