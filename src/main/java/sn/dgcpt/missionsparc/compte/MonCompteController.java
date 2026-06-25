package sn.dgcpt.missionsparc.compte;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Changement de mot de passe en self-service (et obligatoire pour les comptes par défaut/réinitialisés). */
@Controller
public class MonCompteController {

    private final CompteService compteService;

    public MonCompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @GetMapping("/mon-mot-de-passe")
    public String formulaire(Model model, Authentication auth) {
        model.addAttribute("force", auth != null && compteService.doitChangerMotDePasse(auth.getName()));
        return "mot-de-passe";
    }

    @PostMapping("/mon-mot-de-passe")
    public String changer(@RequestParam String actuel, @RequestParam String nouveau,
                          @RequestParam String confirmation, Authentication auth, RedirectAttributes ra) {
        try {
            if (!nouveau.equals(confirmation))
                throw new IllegalArgumentException("La confirmation ne correspond pas au nouveau mot de passe.");
            compteService.changerMonMotDePasse(auth.getName(), actuel, nouveau);
            ra.addFlashAttribute("message", "Mot de passe modifié.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/mon-mot-de-passe";
        }
    }
}
