package sn.dgcpt.missionsparc.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import sn.dgcpt.missionsparc.consultation.ConsultationService;
import sn.dgcpt.missionsparc.consultation.ParcExporter;
import sn.dgcpt.missionsparc.domain.TypeMateriel;

import java.io.IOException;
import java.time.LocalDate;

@Controller
public class ConsultationController {

    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ConsultationService consultation;
    private final ParcExporter parcExporter;

    public ConsultationController(ConsultationService consultation, ParcExporter parcExporter) {
        this.consultation = consultation;
        this.parcExporter = parcExporter;
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
    public String parc(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Integer poste,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String statut,
                       Model model) {
        model.addAttribute("materiels", consultation.listerParc(q, poste, type, statut));
        model.addAttribute("postes", consultation.listerPostes());
        model.addAttribute("types", TypeMateriel.values());
        model.addAttribute("q", q);
        model.addAttribute("fPoste", poste);
        model.addAttribute("fType", type);
        model.addAttribute("fStatut", statut);
        return "parc";
    }

    @GetMapping("/parc/export")
    public ResponseEntity<byte[]> exportParc(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Integer poste,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String statut) throws IOException {
        byte[] data = parcExporter.exporter(consultation.listerParc(q, poste, type, statut));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Parc-" + LocalDate.now() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(TYPE_XLSX))
                .body(data);
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
