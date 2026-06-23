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
import sn.dgcpt.missionsparc.importation.ImportService;
import sn.dgcpt.missionsparc.importation.RapportImport;

import java.io.InputStream;

@Controller
@RequestMapping("/import")
public class ImportController {

    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String formulaire() {
        return "import";
    }

    @GetMapping("/guide")
    public String guide() {
        return "guide";
    }

    /** Télécharge un canevas vierge embarqué dans l'application. */
    @GetMapping("/canevas")
    public ResponseEntity<Resource> telechargerCanevas() {
        Resource canevas = new ClassPathResource("canevas/canevas-vierge.xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Canevas-Saisie-Mission-Parc.xlsx\"")
                .contentType(MediaType.parseMediaType(TYPE_XLSX))
                .body(canevas);
    }

    @PostMapping
    public String televerser(@RequestParam("fichier") MultipartFile fichier, Model model) {
        if (fichier == null || fichier.isEmpty()) {
            model.addAttribute("erreur", "Veuillez sélectionner un fichier .xlsx.");
            return "import";
        }
        try (InputStream is = fichier.getInputStream()) {
            RapportImport rapport = importService.analyser(is);
            model.addAttribute("rapport", rapport);
            model.addAttribute("nomFichier", fichier.getOriginalFilename());
            return "import-resultat";
        } catch (Exception e) {
            model.addAttribute("erreur", "Lecture impossible : " + e.getMessage());
            return "import";
        }
    }
}
