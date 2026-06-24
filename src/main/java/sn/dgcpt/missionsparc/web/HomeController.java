package sn.dgcpt.missionsparc.web;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;
    private final MaterielRepository materielRepo;
    private final MissionRepository missionRepo;

    public HomeController(PosteRepository posteRepo, AgentRepository agentRepo,
                          MaterielRepository materielRepo, MissionRepository missionRepo) {
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
        this.materielRepo = materielRepo;
        this.missionRepo = missionRepo;
    }

    @GetMapping("/")
    @Transactional(readOnly = true)
    public String index(Model model) {
        model.addAttribute("nbPostes", posteRepo.count());
        model.addAttribute("nbInfo", agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN).size());
        model.addAttribute("nbAgentsPoste", agentRepo.findByTypeAgent(TypeAgent.POSTE).size());

        List<Materiel> parc = materielRepo.findAll();
        model.addAttribute("nbMateriel", parc.size());
        model.addAttribute("matEnService", parc.stream().filter(m -> m.getStatut() == StatutMateriel.EN_SERVICE).count());
        model.addAttribute("matEnPanne", parc.stream().filter(m -> m.getStatut() == StatutMateriel.EN_PANNE).count());
        model.addAttribute("matAChanger", parc.stream().filter(m -> m.getStatut() == StatutMateriel.A_CHANGER).count());
        model.addAttribute("nbOrdi", parc.stream().filter(m -> m.getType() == TypeMateriel.ORDINATEUR).count());
        model.addAttribute("nbImp", parc.stream().filter(m -> m.getType() == TypeMateriel.IMPRIMANTE).count());
        model.addAttribute("nbReseau", parc.stream().filter(m -> m.getType() == TypeMateriel.SWITCH || m.getType() == TypeMateriel.ACCESS_POINT).count());
        model.addAttribute("nbScan", parc.stream().filter(m -> m.getType() == TypeMateriel.SCANNER_CHEQUE).count());

        List<Mission> missions = missionRepo.findAll();
        LocalDate today = LocalDate.now();
        long plan = missions.stream().filter(m -> m.getDateDebut() != null && m.getDateDebut().isAfter(today)).count();
        long enCours = missions.stream().filter(m ->
                !(m.getDateDebut() != null && m.getDateDebut().isAfter(today))
                && (m.getDateFin() == null || !m.getDateFin().isBefore(today))).count();
        model.addAttribute("nbMissions", missions.size());
        model.addAttribute("misPlan", plan);
        model.addAttribute("misEnCours", enCours);
        model.addAttribute("misTerm", missions.size() - plan - enCours);
        return "index";
    }
}
