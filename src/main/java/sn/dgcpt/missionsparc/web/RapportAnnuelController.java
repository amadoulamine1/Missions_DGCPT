package sn.dgcpt.missionsparc.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sn.dgcpt.missionsparc.consultation.RapportAnnuelExporter;
import sn.dgcpt.missionsparc.consultation.RapportAnnuelService;
import sn.dgcpt.missionsparc.consultation.dto.RapportAnnuelVue;

import java.io.IOException;
import java.time.LocalDate;

/** Rubrique « Rapport annuel » : revue de pilotage (ADMIN + MANAGER). */
@Controller
public class RapportAnnuelController {

    private final RapportAnnuelService service;
    private final RapportAnnuelExporter exporter;

    public RapportAnnuelController(RapportAnnuelService service, RapportAnnuelExporter exporter) {
        this.service = service;
        this.exporter = exporter;
    }

    @GetMapping("/rapport-annuel")
    public String rapport(@RequestParam(required = false) Integer annee,
                          @RequestParam(required = false) Integer ans, Model model) {
        int y = (annee == null) ? LocalDate.now().getYear() : annee;
        int w = (ans == null) ? 5 : ans;
        model.addAttribute("r", service.rapport(y, w));
        return "rapport-annuel";
    }

    @GetMapping("/rapport-annuel/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Integer annee,
                                         @RequestParam(required = false) Integer ans) throws IOException {
        int y = (annee == null) ? LocalDate.now().getYear() : annee;
        int w = (ans == null) ? 5 : ans;
        RapportAnnuelVue v = service.rapport(y, w);
        byte[] x = exporter.exporter(v);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport-annuel-" + y + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(x);
    }
}
