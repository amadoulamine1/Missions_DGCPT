package sn.dgcpt.missionsparc.web;

import jakarta.servlet.http.HttpSession;
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
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;

import java.io.ByteArrayInputStream;

@Controller
@RequestMapping("/import")
public class ImportController {

    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SESSION_FICHIER = "canevasBytes";
    private static final String SESSION_NOM = "canevasNom";

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

    @GetMapping("/canevas")
    public ResponseEntity<Resource> telechargerCanevas() {
        Resource canevas = new ClassPathResource("canevas/canevas-vierge.xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Canevas-Saisie-Mission-Parc.xlsx\"")
                .contentType(MediaType.parseMediaType(TYPE_XLSX))
                .body(canevas);
    }

    /** Étape 1 : chargement -> lecture + contrôle -> aperçu (rien n'est encore intégré). */
    @PostMapping
    public String televerser(@RequestParam("fichier") MultipartFile fichier,
                             HttpSession session, Model model) {
        if (fichier == null || fichier.isEmpty()) {
            model.addAttribute("erreur", "Veuillez sélectionner un fichier .xlsx.");
            return "import";
        }
        try {
            byte[] bytes = fichier.getBytes();
            CanevasImporte canevas = importService.lire(new ByteArrayInputStream(bytes));
            RapportImport rapport = importService.controler(canevas);
            session.setAttribute(SESSION_FICHIER, bytes);
            session.setAttribute(SESSION_NOM, fichier.getOriginalFilename());
            model.addAttribute("canevas", canevas);
            model.addAttribute("rapport", rapport);
            model.addAttribute("nomFichier", fichier.getOriginalFilename());
            return "import-apercu";
        } catch (Exception e) {
            model.addAttribute("erreur", "Lecture impossible : " + e.getMessage());
            return "import";
        }
    }

    /** Étape 2 : validation explicite -> intégration. */
    @PostMapping("/valider")
    public String valider(HttpSession session, Model model) {
        byte[] bytes = (byte[]) session.getAttribute(SESSION_FICHIER);
        if (bytes == null) {
            model.addAttribute("erreur", "Aucun canevas en attente. Veuillez recharger le fichier.");
            return "import";
        }
        try {
            CanevasImporte canevas = importService.lire(new ByteArrayInputStream(bytes));
            RapportImport rapport = importService.controler(canevas);
            if (!rapport.estIntegrable()) {
                model.addAttribute("canevas", canevas);
                model.addAttribute("rapport", rapport);
                model.addAttribute("nomFichier", session.getAttribute(SESSION_NOM));
                model.addAttribute("erreur", "Des anomalies bloquantes empêchent la validation.");
                return "import-apercu";
            }
            int nbMateriels = importService.integrer(canevas);
            session.removeAttribute(SESSION_FICHIER);
            session.removeAttribute(SESSION_NOM);
            model.addAttribute("reference", canevas.getEntete().getReference());
            model.addAttribute("nbMateriels", nbMateriels);
            model.addAttribute("nbMembres", canevas.getMembres().size());
            return "import-valide";
        } catch (Exception e) {
            model.addAttribute("erreur", "Validation impossible : " + e.getMessage());
            return "import";
        }
    }
}
