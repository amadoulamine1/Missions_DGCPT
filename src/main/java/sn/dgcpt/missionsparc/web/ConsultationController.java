package sn.dgcpt.missionsparc.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sn.dgcpt.missionsparc.consultation.ConsultationService;

@Controller
public class ConsultationController {

    private final ConsultationService consultation;

    public ConsultationController(ConsultationService consultation) {
        this.consultation = consultation;
    }

    @GetMapping("/postes")
    public String postes(Model model) {
        model.addAttribute("postes", consultation.listerPostes());
        return "postes";
    }

    @GetMapping("/postes/{id}")
    public String poste(@PathVariable Integer id, Model model) {
        model.addAttribute("d", consultation.detailPoste(id));
        return "poste-detail";
    }

    @GetMapping("/parc")
    public String parc(Model model) {
        model.addAttribute("materiels", consultation.listerParc());
        return "parc";
    }

    @GetMapping("/parc/{numero}")
    public String materiel(@PathVariable String numero, Model model) {
        model.addAttribute("d", consultation.detailMateriel(numero));
        return "materiel-detail";
    }

    @GetMapping("/missions")
    public String missions(Model model) {
        model.addAttribute("missions", consultation.listerMissions());
        return "missions";
    }

    @GetMapping("/missions/{id}")
    public String mission(@PathVariable Integer id, Model model) {
        model.addAttribute("d", consultation.detailMission(id));
        return "mission-detail";
    }
}
