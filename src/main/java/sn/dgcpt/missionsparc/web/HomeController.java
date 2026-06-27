package sn.dgcpt.missionsparc.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sn.dgcpt.missionsparc.consultation.StatsExporter;
import sn.dgcpt.missionsparc.consultation.dto.StatPoste;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;
    private final MaterielRepository materielRepo;
    private final MissionRepository missionRepo;
    private final StatsExporter statsExporter;

    public HomeController(PosteRepository posteRepo, AgentRepository agentRepo,
                          MaterielRepository materielRepo, MissionRepository missionRepo,
                          StatsExporter statsExporter) {
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
        this.materielRepo = materielRepo;
        this.missionRepo = missionRepo;
        this.statsExporter = statsExporter;
    }

    @GetMapping("/")
    @Transactional(readOnly = true)
    public String index(@RequestParam(required = false) Integer poste, Model model) {
        List<Poste> postes = posteRepo.findAll();
        model.addAttribute("postes", postes);
        model.addAttribute("fPoste", poste);
        model.addAttribute("posteNom", poste == null ? null :
                postes.stream().filter(p -> p.getId().equals(poste)).map(Poste::getNom).findFirst().orElse(null));

        model.addAttribute("nbPostes", postes.size());
        model.addAttribute("nbInfo", agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN).size());
        model.addAttribute("nbAgentsPoste", agentRepo.findByTypeAgent(TypeAgent.POSTE).size());

        List<Materiel> fullParc = materielRepo.findAll();
        List<Materiel> parc = (poste == null) ? fullParc
                : fullParc.stream().filter(m -> m.getPoste() != null && poste.equals(m.getPoste().getId())).toList();

        long matEnService = parc.stream().filter(m -> m.getStatut() == StatutMateriel.EN_SERVICE).count();
        long matEnPanne = parc.stream().filter(m -> m.getStatut() == StatutMateriel.EN_PANNE).count();
        long matAChanger = parc.stream().filter(m -> m.getStatut() == StatutMateriel.A_CHANGER).count();
        model.addAttribute("nbMateriel", parc.size());
        model.addAttribute("matEnService", matEnService);
        model.addAttribute("matEnPanne", matEnPanne);
        model.addAttribute("matAChanger", matAChanger);
        long nbStatues = matEnService + matEnPanne + matAChanger;
        model.addAttribute("nbStatues", nbStatues);
        model.addAttribute("tauxDispo", nbStatues > 0 ? matEnService * 100 / nbStatues : 0);

        long nbOrdi = parc.stream().filter(m -> m.getType() == TypeMateriel.ORDINATEUR).count();
        long nbImp = parc.stream().filter(m -> m.getType() == TypeMateriel.IMPRIMANTE).count();
        long nbReseau = parc.stream().filter(m -> m.getType() == TypeMateriel.SWITCH || m.getType() == TypeMateriel.ACCESS_POINT).count();
        long nbScan = parc.stream().filter(m -> m.getType() == TypeMateriel.SCANNER_CHEQUE).count();
        model.addAttribute("nbOrdi", nbOrdi);
        model.addAttribute("nbImp", nbImp);
        model.addAttribute("nbReseau", nbReseau);
        model.addAttribute("nbScan", nbScan);
        model.addAttribute("maxType", Math.max(Math.max(nbOrdi, nbImp), Math.max(nbReseau, nbScan)));
        model.addAttribute("ordSvc", c(parc, StatutMateriel.EN_SERVICE, TypeMateriel.ORDINATEUR));
        model.addAttribute("ordPan", c(parc, StatutMateriel.EN_PANNE, TypeMateriel.ORDINATEUR));
        model.addAttribute("ordChg", c(parc, StatutMateriel.A_CHANGER, TypeMateriel.ORDINATEUR));
        model.addAttribute("impSvc", c(parc, StatutMateriel.EN_SERVICE, TypeMateriel.IMPRIMANTE));
        model.addAttribute("impPan", c(parc, StatutMateriel.EN_PANNE, TypeMateriel.IMPRIMANTE));
        model.addAttribute("impChg", c(parc, StatutMateriel.A_CHANGER, TypeMateriel.IMPRIMANTE));
        model.addAttribute("resSvc", c(parc, StatutMateriel.EN_SERVICE, TypeMateriel.SWITCH, TypeMateriel.ACCESS_POINT));
        model.addAttribute("resPan", c(parc, StatutMateriel.EN_PANNE, TypeMateriel.SWITCH, TypeMateriel.ACCESS_POINT));
        model.addAttribute("resChg", c(parc, StatutMateriel.A_CHANGER, TypeMateriel.SWITCH, TypeMateriel.ACCESS_POINT));
        model.addAttribute("scnSvc", c(parc, StatutMateriel.EN_SERVICE, TypeMateriel.SCANNER_CHEQUE));
        model.addAttribute("scnPan", c(parc, StatutMateriel.EN_PANNE, TypeMateriel.SCANNER_CHEQUE));
        model.addAttribute("scnChg", c(parc, StatutMateriel.A_CHANGER, TypeMateriel.SCANNER_CHEQUE));

        // Vue globale uniquement : matériel par poste + postes en alerte (calculés sur tout le parc)
        List<StatPoste> parPosteAll = StatsExporter.parPoste(fullParc);
        List<StatPoste> parPoste = parPosteAll.stream().limit(8).toList();
        model.addAttribute("parPoste", parPoste);
        model.addAttribute("maxParPoste", parPoste.stream().mapToLong(StatPoste::getTotal).max().orElse(0));
        model.addAttribute("postesAlerte", parPosteAll.stream().filter(sp -> sp.getEnPanne() > 0).count());

        List<Mission> missions = missionRepo.findAll();
        if (poste != null) {
            missions = missions.stream().filter(m -> m.getPoste() != null && poste.equals(m.getPoste().getId())).toList();
        }
        LocalDate today = LocalDate.now();
        long plan = missions.stream().filter(m -> m.getDateDebut() != null && m.getDateDebut().isAfter(today)).count();
        long enCours = missions.stream().filter(m ->
                !(m.getDateDebut() != null && m.getDateDebut().isAfter(today))
                && (m.getDateFin() == null || !m.getDateFin().isBefore(today))).count();
        model.addAttribute("nbMissions", missions.size());
        model.addAttribute("misPlan", plan);
        model.addAttribute("misEnCours", enCours);
        model.addAttribute("misTerm", missions.size() - plan - enCours);

        // Alertes proactives : missions dont la date de fin est dépassée mais non clôturées (en retard),
        // et missions en cours dont la date de fin approche (échéance ≤ 7 jours).
        LocalDate dans7 = today.plusDays(7);
        long misEnRetard = missions.stream().filter(m -> m.getStatut() != StatutMission.CLOTUREE
                && m.getDateFin() != null && m.getDateFin().isBefore(today)).count();
        long misEcheance = missions.stream().filter(m -> m.getStatut() != StatutMission.CLOTUREE
                && m.getDateFin() != null && !m.getDateFin().isBefore(today) && !m.getDateFin().isAfter(dans7)).count();
        model.addAttribute("misEnRetard", misEnRetard);
        model.addAttribute("misEcheance", misEcheance);
        return "index";
    }

    @GetMapping("/tableau-bord/export")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportStats(@RequestParam(required = false) Integer poste) throws IOException {
        List<Materiel> parc = materielRepo.findAll();
        List<Mission> missions = missionRepo.findAll();
        long nbPostes = posteRepo.count();
        if (poste != null) {
            parc = parc.stream().filter(m -> m.getPoste() != null && poste.equals(m.getPoste().getId())).toList();
            missions = missions.stream().filter(m -> m.getPoste() != null && poste.equals(m.getPoste().getId())).toList();
            nbPostes = 1;
        }
        byte[] x = statsExporter.exporter(parc, missions, nbPostes,
                agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN).size(),
                agentRepo.findByTypeAgent(TypeAgent.POSTE).size());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statistiques-dgcpt.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(x);
    }

    private long c(List<Materiel> parc, StatutMateriel st, TypeMateriel... types) {
        return parc.stream().filter(m -> m.getStatut() == st && contient(types, m.getType())).count();
    }

    private boolean contient(TypeMateriel[] types, TypeMateriel t) {
        for (TypeMateriel x : types) if (x == t) return true;
        return false;
    }
}
