package sn.dgcpt.missionsparc.web;

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

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String formulaire() {
        return "import";
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
