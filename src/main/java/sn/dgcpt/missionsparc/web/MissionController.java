package sn.dgcpt.missionsparc.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.OrdreMission;
import sn.dgcpt.missionsparc.mission.CreationMissionForm;
import sn.dgcpt.missionsparc.mission.EditionMissionForm;
import sn.dgcpt.missionsparc.mission.MissionService;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
public class MissionController {

    private final MissionService missionService;
    private final PosteRepository posteRepo;

    public MissionController(MissionService missionService, PosteRepository posteRepo) {
        this.missionService = missionService;
        this.posteRepo = posteRepo;
    }

    private void garnir(Model model) {
        model.addAttribute("postes", posteRepo.findAll());
        model.addAttribute("informaticiens", missionService.informaticiens());
        model.addAttribute("tprData", missionService.tprData());
    }

    @GetMapping("/missions/nouvelle")
    public String formulaire(Model model) {
        model.addAttribute("form", new CreationMissionForm());
        garnir(model);
        return "mission-nouvelle";
    }

    @PostMapping("/missions/nouvelle")
    public String creer(@ModelAttribute("form") CreationMissionForm form, Model model) {
        try {
            Mission m = missionService.creer(form);
            return "redirect:/missions/" + m.getId();
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            model.addAttribute("form", form);
            model.addAttribute("erreur", e.getMessage() != null ? e.getMessage() : "Données invalides.");
            garnir(model);
            return "mission-nouvelle";
        }
    }

    @GetMapping("/missions/{id}/modifier")
    public String editer(@PathVariable Integer id, Model model) {
        model.addAttribute("form", missionService.formulaireEdition(id));
        model.addAttribute("informaticiens", missionService.informaticiens());
        model.addAttribute("ref", missionService.reference(id));
        return "mission-edit";
    }

    @PostMapping("/missions/{id}/modifier")
    public String modifier(@PathVariable Integer id, @ModelAttribute("form") EditionMissionForm form, Model model) {
        form.setId(id);
        try {
            missionService.modifier(form);
            return "redirect:/missions/" + id;
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            model.addAttribute("erreur", e.getMessage() != null ? e.getMessage() : "Données invalides.");
            model.addAttribute("informaticiens", missionService.informaticiens());
            model.addAttribute("ref", missionService.reference(id));
            return "mission-edit";
        }
    }

    @PostMapping("/missions/{id}/membres/{matricule}/retirer")
    public String retirerMembre(@PathVariable Integer id, @PathVariable String matricule, RedirectAttributes ra) {
        missionService.retirerMembre(id, matricule);
        ra.addFlashAttribute("message", "Membre retiré de la mission.");
        return "redirect:/missions/" + id;
    }

    @PostMapping("/missions/{id}/cloturer")
    public String cloturer(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            missionService.cloturer(id);
            ra.addFlashAttribute("message", "Mission clôturée.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/missions/" + id;
    }

    @GetMapping("/missions/{id}/canevas")
    public ResponseEntity<byte[]> canevas(@PathVariable Integer id) throws IOException {
        byte[] data = missionService.genererCanevasZip(id);
        String nom = missionService.nomZipMission(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(data);
    }

    /** Joindre un ordre de mission au format PDF (plusieurs possibles par mission). */
    @PostMapping("/missions/{id}/ordre")
    public String joindreOrdre(@PathVariable Integer id, @RequestParam("fichier") MultipartFile fichier, RedirectAttributes ra) {
        try {
            missionService.attacherOrdre(id, fichier);
            ra.addFlashAttribute("message", "Ordre de mission attaché.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        } catch (IOException e) {
            ra.addFlashAttribute("erreur", "Impossible de lire le fichier envoyé.");
        }
        return "redirect:/missions";
    }

    /** Télécharger un ordre de mission (PDF) par son identifiant : ouverture dans le navigateur. */
    @GetMapping("/missions/{id}/ordre/{ordreId}")
    public ResponseEntity<byte[]> telechargerOrdre(@PathVariable Integer id, @PathVariable Integer ordreId) {
        OrdreMission o = missionService.ordreMission(ordreId).orElse(null);
        if (o == null || !id.equals(o.getMissionId())) return ResponseEntity.notFound().build();
        String nom = o.getNomFichier() == null ? "ordre-mission.pdf" : o.getNomFichier();
        String ascii = nom.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "");
        String dispo = "inline; filename=\"" + ascii + "\"; filename*=UTF-8''"
                + java.net.URLEncoder.encode(nom, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(o.getContenu());
    }

    /** Supprimer un ordre de mission par son identifiant. */
    @PostMapping("/missions/{id}/ordre/{ordreId}/supprimer")
    public String supprimerOrdre(@PathVariable Integer id, @PathVariable Integer ordreId, RedirectAttributes ra) {
        missionService.supprimerOrdre(ordreId);
        ra.addFlashAttribute("message", "Ordre de mission supprimé.");
        return "redirect:/missions";
    }

    /** Téléchargement en lot : les canevas de plusieurs missions dans un seul ZIP. */
    @GetMapping("/missions/canevas")
    public ResponseEntity<byte[]> canevasLot(@RequestParam("id") List<Integer> ids) throws IOException {
        byte[] data = missionService.genererCanevasZipLot(ids);
        String nom = "Canevas-missions-" + LocalDate.now();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(data);
    }
}
