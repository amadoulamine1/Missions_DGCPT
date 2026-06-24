package sn.dgcpt.missionsparc.consultation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.consultation.dto.*;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.time.LocalDate;
import java.util.List;

/** Lecture seule : restitutions (postes, parc, missions). Mappe vers des DTO de vue. */
@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private final PosteRepository posteRepo;
    private final MaterielRepository materielRepo;
    private final ChefPosteRepository chefPosteRepo;
    private final AgentRepository agentRepo;
    private final MissionRepository missionRepo;
    private final ReleveMaterielRepository releveRepo;

    public ConsultationService(PosteRepository posteRepo, MaterielRepository materielRepo,
                               ChefPosteRepository chefPosteRepo, AgentRepository agentRepo,
                               MissionRepository missionRepo, ReleveMaterielRepository releveRepo) {
        this.posteRepo = posteRepo;
        this.materielRepo = materielRepo;
        this.chefPosteRepo = chefPosteRepo;
        this.agentRepo = agentRepo;
        this.missionRepo = missionRepo;
        this.releveRepo = releveRepo;
    }

    // ---- Postes ----

    public List<PosteVue> listerPostes() {
        return posteRepo.findAll().stream().map(this::versPosteVue).toList();
    }

    public PosteDetailVue detailPoste(Integer id) {
        Poste p = posteRepo.findById(id).orElseThrow();
        var courant = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(id);
        String chef = courant.map(cp -> cp.getAgent().getMatricule() + " — " + cp.getAgent().getNom()).orElse("(aucun)");
        String chefMat = courant.map(cp -> cp.getAgent().getMatricule()).orElse("");
        List<AgentVue> agents = agentRepo.findByPoste_Id(id).stream().map(this::versAgentVue).toList();
        List<MaterielVue> materiels = materielRepo.findByPoste_Id(id).stream().map(this::versMaterielVue).toList();
        return new PosteDetailVue(versPosteVue(p), chef, chefMat, agents, materiels);
    }

    // ---- Parc ----

    public List<MaterielVue> listerParc() {
        return materielRepo.findAll().stream().map(this::versMaterielVue).toList();
    }

    // ---- Missions ----

    public List<MissionVue> listerMissions() {
        return missionRepo.findAll().stream().map(this::versMissionVue).toList();
    }

    public MissionDetailVue detailMission(Integer id) {
        Mission m = missionRepo.findById(id).orElseThrow();
        String chefMission = m.getChefMission() == null ? "" : m.getChefMission().getMatricule();
        String chefPoste = m.getChefPosteFige() == null ? "" : m.getChefPosteFige().getMatricule();
        String cable = m.getCategorieCable() == null ? "" : m.getCategorieCable().getLibelle();
        List<ReleveVue> releves = releveRepo.findByMission_Id(id).stream().map(this::versReleveVue).toList();
        return new MissionDetailVue(versMissionVue(m), chefMission, chefPoste, m.getEtatCablage(), cable, releves);
    }

    // ---- mappers ----

    private PosteVue versPosteVue(Poste p) {
        return new PosteVue(p.getId(), p.getCode(), p.getNom(), p.getRegion(), materielRepo.countByPoste_Id(p.getId()));
    }

    private AgentVue versAgentVue(Agent a) {
        return new AgentVue(a.getMatricule(), a.getNom() + " " + a.getPrenom(), a.getFonction(), a.getTelephone());
    }

    private MaterielVue versMaterielVue(Materiel m) {
        String poste = m.getPoste() == null ? "" : m.getPoste().getNom();
        return new MaterielVue(m.getNumeroInventaire(), m.getType().name(), m.getNom(), m.getModele(), poste);
    }

    private MissionVue versMissionVue(Mission m) {
        String poste = m.getPoste() == null ? "" : m.getPoste().getNom();
        return new MissionVue(m.getId(), m.getReference(), m.getObjet(), poste, periode(m.getDateDebut(), m.getDateFin()), m.getStatut().name());
    }

    private ReleveVue versReleveVue(ReleveMateriel r) {
        Materiel m = r.getMateriel();
        String saisisseur = r.getAgentSaisisseur() == null ? "" : r.getAgentSaisisseur().getMatricule();
        return new ReleveVue(m.getNumeroInventaire(), m.getType().name(), m.getNom(), saisisseur, r.getZone(), r.getDateReleve());
    }

    private String periode(LocalDate d1, LocalDate d2) {
        String a = (d1 == null) ? "?" : d1.toString();
        String b = (d2 == null) ? "…" : d2.toString();
        return a + " → " + b;
    }
}
