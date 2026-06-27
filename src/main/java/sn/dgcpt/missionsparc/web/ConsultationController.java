package sn.dgcpt.missionsparc.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.consultation.ConsultationService;
import sn.dgcpt.missionsparc.consultation.Pagination;
import sn.dgcpt.missionsparc.consultation.dto.MaterielVue;
import sn.dgcpt.missionsparc.consultation.dto.MissionVue;
import sn.dgcpt.missionsparc.consultation.dto.PageVue;
import sn.dgcpt.missionsparc.consultation.dto.PosteVue;
import sn.dgcpt.missionsparc.consultation.ParcExporter;
import sn.dgcpt.missionsparc.affectation.AffectationService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
public class ConsultationController {

    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ConsultationService consultation;
    private final ParcExporter parcExporter;
    private final AffectationService affectationService;

    public ConsultationController(ConsultationService consultation, ParcExporter parcExporter,
                                  AffectationService affectationService) {
        this.consultation = consultation;
        this.parcExporter = parcExporter;
        this.affectationService = affectationService;
    }

    @GetMapping("/postes")
    public String postes(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String tri,
                         @RequestParam(required = false) String sens,
                         Model model) {
        List<PosteVue> tout = consultation.listerPostes();
        Comparator<PosteVue> cmp = comparateurPostes(tri);
        if ("desc".equals(sens)) cmp = cmp.reversed();
        PageVue<PosteVue> p = Pagination.page(tout, page, 25, cmp);
        model.addAttribute("page", p);
        model.addAttribute("postes", p.getContenu());
        model.addAttribute("tri", tri);
        model.addAttribute("sens", sens);
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
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String tri,
                       @RequestParam(required = false) String sens,
                       Model model) {
        PageVue<MaterielVue> p = consultation.listerParcPage(q, poste, type, statut,
                PageRequest.of(Math.max(0, page), 25, triParc(tri, sens)));
        model.addAttribute("page", p);
        model.addAttribute("materiels", p.getContenu());
        model.addAttribute("postes", consultation.listerPostes());
        model.addAttribute("types", consultation.typesMateriel());
        model.addAttribute("q", q);
        model.addAttribute("fPoste", poste);
        model.addAttribute("fType", type);
        model.addAttribute("fStatut", statut);
        model.addAttribute("tri", tri);
        model.addAttribute("sens", sens);
        return "parc";
    }

    /** Tri du parc côté base : propriété d'entité (insensible à la casse, NULL en tête) selon la colonne. */
    private Sort triParc(String tri, String sens) {
        String prop = switch (tri == null ? "" : tri) {
            case "type" -> "categorie.libelle";
            case "nom" -> "nom";
            case "modele" -> "modele";
            case "poste" -> "poste.nom";
            case "statut" -> "statut";
            default -> "numeroInventaire";
        };
        Sort.Direction dir = "desc".equals(sens) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(new Sort.Order(dir, prop).ignoreCase().nullsFirst());
    }

    private Comparator<PosteVue> comparateurPostes(String tri) {
        Comparator<String> n = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        return switch (tri == null ? "" : tri) {
            case "nom" -> Comparator.comparing(PosteVue::getNom, n);
            case "region" -> Comparator.comparing(PosteVue::getRegion, n);
            case "materiel" -> Comparator.comparingLong(PosteVue::getNbMateriel);
            default -> Comparator.comparing(PosteVue::getCode, n);
        };
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

    @GetMapping("/inventaire")
    public String inventaireDate(@RequestParam(required = false) String d, Model model) {
        LocalDate date;
        try { date = (d == null || d.isBlank()) ? LocalDate.now() : LocalDate.parse(d); }
        catch (Exception e) { date = LocalDate.now(); }
        model.addAttribute("date", date.toString());
        model.addAttribute("lignes", consultation.inventaireALaDate(date));
        return "inventaire-date";
    }

    @GetMapping("/parc/etiquettes")
    public String etiquettes(@RequestParam(required = false) String q,
                             @RequestParam(required = false) Integer poste,
                             @RequestParam(required = false) String type,
                             @RequestParam(required = false) String statut,
                             Model model) {
        model.addAttribute("materiels", consultation.listerParc(q, poste, type, statut));
        return "etiquettes";
    }

    @GetMapping("/parc/{numero}")
    public String materiel(@PathVariable String numero, Model model) {
        model.addAttribute("d", consultation.detailMateriel(numero));
        model.addAttribute("candidats", consultation.candidatsAffectation(numero));
        return "materiel-detail";
    }

    @PostMapping("/parc/{numero}/affectation")
    public String reaffecter(@PathVariable String numero,
                             @RequestParam String agent,
                             @RequestParam(required = false) String date,
                             RedirectAttributes ra) {
        try {
            LocalDate d = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
            affectationService.reaffecter(numero, agent, d);
            ra.addFlashAttribute("message", "Matériel réaffecté.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/parc/" + numero;
    }

    @GetMapping("/missions")
    public String missions(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Integer poste,
                           @RequestParam(required = false) String region,
                           @RequestParam(required = false) String agent,
                           @RequestParam(required = false) String etat,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String tri,
                           @RequestParam(required = false) String sens,
                           Model model) {
        // Tri par défaut : N° de mission décroissant (missions les plus récentes en premier)
        if (tri == null && sens == null) { tri = "ref"; sens = "desc"; }
        List<MissionVue> tout = consultation.listerMissions(q, poste, region, agent, etat);
        Comparator<MissionVue> cmp = comparateurMissions(tri);
        if ("desc".equals(sens)) cmp = cmp.reversed();
        PageVue<MissionVue> p = Pagination.page(tout, page, 25, cmp);
        model.addAttribute("page", p);
        model.addAttribute("missions", p.getContenu());
        model.addAttribute("postes", consultation.listerPostes());
        model.addAttribute("regions", consultation.listerRegions());
        model.addAttribute("agents", consultation.listerInformaticiens());
        model.addAttribute("q", q);
        model.addAttribute("fPoste", poste);
        model.addAttribute("fRegion", region);
        model.addAttribute("fAgent", agent);
        model.addAttribute("fEtat", etat);
        model.addAttribute("tri", tri);
        model.addAttribute("sens", sens);
        return "missions";
    }

    private Comparator<MissionVue> comparateurMissions(String tri) {
        Comparator<String> n = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        return switch (tri == null ? "" : tri) {
            case "objet" -> Comparator.comparing(MissionVue::getObjet, n);
            case "poste" -> Comparator.comparing(MissionVue::getPosteNom, n);
            case "etat" -> Comparator.comparing(MissionVue::getEtat, n);
            case "statut" -> Comparator.comparing(MissionVue::getStatut, n);
            default -> Comparator.comparing(MissionVue::getReference, n);
        };
    }

    @GetMapping("/missions/{id}")
    public String mission(@PathVariable Integer id, Model model) {
        model.addAttribute("d", consultation.detailMission(id));
        return "mission-detail";
    }

    @GetMapping("/missions/{id}/releves/export")
    public ResponseEntity<byte[]> exportReleves(@PathVariable Integer id) throws IOException {
        byte[] data = parcExporter.exporterReleves(consultation.relevesDeMission(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Releves-mission-" + id + ".xlsx\"")
                .contentType(MediaType.parseMediaType(TYPE_XLSX))
                .body(data);
    }
}
