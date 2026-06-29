package sn.dgcpt.missionsparc.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.donnees.DonneesService;

/** Export / import de la base de données — réservé à l'administrateur (cf. SecurityConfig : /donnees/**). */
@Controller
public class DonneesController {

    private final DonneesService donnees;

    public DonneesController(DonneesService donnees) {
        this.donnees = donnees;
    }

    @GetMapping("/donnees")
    public String page() {
        return "donnees";
    }

    /** Télécharge une sauvegarde complète de la base (format custom restaurable). */
    @PostMapping("/donnees/export")
    public Object exporter(RedirectAttributes ra) {
        try {
            byte[] dump = donnees.exporter();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + donnees.nomFichierExport() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(dump);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/donnees";
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", "Export impossible : " + e.getMessage());
            return "redirect:/donnees";
        }
    }

    /** Restaure la base à partir d'un fichier de sauvegarde téléversé. Remplace les données. */
    @PostMapping("/donnees/import")
    public String importer(@RequestParam("fichier") MultipartFile fichier, RedirectAttributes ra) {
        java.io.File tmp = null;
        try {
            if (fichier == null || fichier.isEmpty()) {
                throw new IllegalArgumentException("Veuillez sélectionner un fichier de sauvegarde (.dump).");
            }
            tmp = java.io.File.createTempFile("import-bd", ".dump");   // évite de charger tout le fichier en mémoire
            fichier.transferTo(tmp);
            donnees.importer(tmp);
            ra.addFlashAttribute("message", "Import terminé : la base a été restaurée à partir du fichier.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", "Import impossible : " + e.getMessage());
        } finally {
            if (tmp != null) tmp.delete();
        }
        return "redirect:/donnees";
    }
}
