package sn.dgcpt.missionsparc.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.mission.PosteForm;
import sn.dgcpt.missionsparc.repository.PosteRepository;

@Controller
@RequestMapping("/postes")
public class PosteController {

    private final PosteRepository posteRepo;

    public PosteController(PosteRepository posteRepo) {
        this.posteRepo = posteRepo;
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
    public String creer(@ModelAttribute("form") PosteForm form, Model model) {
        String code = form.getCode() == null ? "" : form.getCode().trim();
        if (posteRepo.findByCode(code).isPresent()) {
            model.addAttribute("form", form);
            model.addAttribute("mode", "creation");
            model.addAttribute("erreur", "Un poste avec le code « " + code + " » existe déjà.");
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
    public String mettreAJour(@PathVariable Integer id, @ModelAttribute("form") PosteForm form) {
        Poste p = posteRepo.findById(id).orElseThrow();
        p.setCode(form.getCode() == null ? p.getCode() : form.getCode().trim());
        p.setNom(form.getNom());
        p.setRegion(form.getRegion());
        posteRepo.save(p);
        return "redirect:/postes";
    }
}
