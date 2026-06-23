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
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.mission.CreationMissionForm;
import sn.dgcpt.missionsparc.mission.MissionService;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.io.IOException;

@Controller
public class MissionController {

    private static final String TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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

    @GetMapping("/missions/{id}/canevas")
    public ResponseEntity<byte[]> canevas(@PathVariable Integer id) throws IOException {
        byte[] data = missionService.genererCanevas(id);
        String ref = missionService.reference(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Canevas-" + ref + ".xlsx\"")
                .contentType(MediaType.parseMediaType(TYPE_XLSX))
                .body(data);
    }
}
