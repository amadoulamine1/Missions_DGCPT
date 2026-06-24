package sn.dgcpt.missionsparc.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.importation.ConsolidationService;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ConsolidationController {

    private final ConsolidationService consolidation;

    public ConsolidationController(ConsolidationService consolidation) {
        this.consolidation = consolidation;
    }

    @GetMapping("/missions/{id}/consolidation")
    public String page(@PathVariable Integer id, Model model) {
        model.addAttribute("missionId", id);
        model.addAttribute("ref", consolidation.reference(id));
        model.addAttribute("lots", consolidation.lots(id));
        model.addAttribute("conflits", consolidation.conflits(id));
        return "consolidation";
    }

    @PostMapping("/missions/{id}/consolidation/integrer")
    public String integrer(@PathVariable Integer id, @RequestParam Map<String, String> params, RedirectAttributes ra) {
        Map<String, Integer> arbitrages = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("g") && e.getValue() != null && e.getValue().contains("::")) {
                String[] p = e.getValue().split("::", 2);
                try { arbitrages.put(p[0], Integer.parseInt(p[1])); } catch (NumberFormatException ignored) { }
            }
        }
        try {
            int n = consolidation.integrer(id, arbitrages);
            ra.addFlashAttribute("message", "Consolidation intégrée : " + n + " matériel(s).");
            return "redirect:/missions/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erreur", ex.getMessage());
            return "redirect:/missions/" + id + "/consolidation";
        }
    }

    @PostMapping("/missions/{id}/consolidation/lots/{lotId}/supprimer")
    public String supprimer(@PathVariable Integer id, @PathVariable Integer lotId, RedirectAttributes ra) {
        consolidation.supprimerLot(lotId);
        ra.addFlashAttribute("message", "Lot retiré de la consolidation.");
        return "redirect:/missions/" + id + "/consolidation";
    }
}
