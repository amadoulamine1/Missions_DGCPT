package sn.dgcpt.missionsparc.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.importation.ConsolidationService;
import sn.dgcpt.missionsparc.importation.ImportService;
import sn.dgcpt.missionsparc.importation.RapportImport;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;

import java.io.ByteArrayInputStream;

@Controller
@RequestMapping("/import")
public class ImportController {

    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ImportService importService;
    private final ConsolidationService consolidation;

    public ImportController(ImportService importService, ConsolidationService consolidation) {
        this.importService = importService;
        this.consolidation = consolidation;
    }

    @GetMapping
    public String formulaire() {
        return "import";
    }

    @GetMapping("/guide")
    public String guide() {
        return "guide";
    }

    @GetMapping("/canevas")
    public ResponseEntity<Resource> telechargerCanevas() {
        Resource canevas = new ClassPathResource("canevas/canevas-vierge.xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Canevas-Saisie-Mission-Parc.xlsx\"")
                .contentType(MediaType.parseMediaType(TYPE_XLSX))
                .body(canevas);
    }

    /** Chargement : lecture + contrôle ; si OK, le fichier devient un lot ajouté à la consolidation de la mission. */
    @PostMapping
    public String televerser(@RequestParam("fichier") MultipartFile fichier, Model model, RedirectAttributes ra) {
        if (fichier == null || fichier.isEmpty()) {
            model.addAttribute("erreur", "Veuillez sélectionner un fichier .xlsx.");
            return "import";
        }
        try {
            byte[] bytes = fichier.getBytes();
            CanevasImporte canevas = importService.lire(new ByteArrayInputStream(bytes));
            RapportImport rapport = importService.controler(canevas);
            if (!rapport.estIntegrable()) {
                model.addAttribute("canevas", canevas);
                model.addAttribute("rapport", rapport);
                model.addAttribute("nomFichier", fichier.getOriginalFilename());
                model.addAttribute("erreur", "Des anomalies bloquantes empêchent l'ajout à la consolidation. Corrigez le canevas puis rechargez-le.");
                return "import-apercu";
            }
            Integer missionId = consolidation.creerLot(canevas, bytes, fichier.getOriginalFilename());
            ra.addFlashAttribute("message", "Fichier ajouté à la consolidation de la mission.");
            return "redirect:/missions/" + missionId + "/consolidation";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erreur", e.getMessage());
            return "import";
        } catch (Exception e) {
            model.addAttribute("erreur", "Lecture impossible : " + e.getMessage());
            return "import";
        }
    }
}
