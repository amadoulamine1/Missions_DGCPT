package sn.dgcpt.missionsparc.mission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.ChefPosteRepository;
import sn.dgcpt.missionsparc.repository.MissionRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MissionService {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private final MissionRepository missionRepo;
    private final PosteRepository posteRepo;
    private final AgentRepository agentRepo;
    private final ChefPosteRepository chefPosteRepo;
    private final ReferentielService referentiel;
    private final CanevasWriter canevasWriter;

    public MissionService(MissionRepository missionRepo, PosteRepository posteRepo, AgentRepository agentRepo,
                          ChefPosteRepository chefPosteRepo, ReferentielService referentiel, CanevasWriter canevasWriter) {
        this.missionRepo = missionRepo;
        this.posteRepo = posteRepo;
        this.agentRepo = agentRepo;
        this.chefPosteRepo = chefPosteRepo;
        this.referentiel = referentiel;
        this.canevasWriter = canevasWriter;
    }

    @Transactional
    public Mission creer(CreationMissionForm f) {
        LocalDate debut = parseDate(f.getDateDebut(), LocalDate.now());
        LocalDate fin = parseDate(f.getDateFin(), null);
        if (fin != null && fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
        }

        Poste poste = (f.getPosteId() != null)
                ? posteRepo.findById(f.getPosteId()).orElseThrow()
                : referentiel.resoudrePoste(f.getCodePoste(), f.getNomPoste());

        Agent chefMission = resoudreChef(f.getChefMissionSel(), f.getChefMissionMat(), f.getChefMissionNom(),
                f.getChefMissionPrenom(), TypeAgent.INFORMATICIEN, null, "chef de mission");
        Agent chefPoste = resoudreChef(f.getChefPosteSel(), f.getChefPosteMat(), f.getChefPosteNom(),
                f.getChefPostePrenom(), TypeAgent.POSTE, poste, "chef de poste");

        Mission m = new Mission();
        m.setReference(genererReference());
        m.setObjet((f.getObjet() == null || f.getObjet().isBlank()) ? "(mission)" : f.getObjet().trim());
        m.setDateDebut(debut);
        m.setDateFin(fin);
        m.setPoste(poste);
        m.setChefMission(chefMission);
        m.setChefPosteFige(chefPoste);
        m.setStatut(StatutMission.EN_CONSOLIDATION);
        Mission saved = missionRepo.save(m);

        historiserChefPoste(poste, chefPoste, debut);
        return saved;
    }

    /** Liste des informaticiens (chef de mission). */
    @Transactional(readOnly = true)
    public List<AgentOption> informaticiens() {
        return agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN).stream().map(this::option).collect(Collectors.toList());
    }

    /** Pour chaque TPR : ses agents + le dernier chef de poste connu (reconduction). */
    @Transactional(readOnly = true)
    public Map<String, TprInfo> tprData() {
        Map<String, TprInfo> data = new LinkedHashMap<>();
        for (Poste p : posteRepo.findAll()) {
            List<AgentOption> agents = agentRepo.findByPoste_Id(p.getId()).stream().map(this::option).collect(Collectors.toList());
            String dernier = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(p.getId())
                    .map(cp -> cp.getAgent().getMatricule()).orElse("");
            data.put(String.valueOf(p.getId()), new TprInfo(agents, dernier));
        }
        return data;
    }

    @Transactional(readOnly = true)
    public byte[] genererCanevas(Integer missionId) throws IOException {
        return canevasWriter.prestamper(missionRepo.findById(missionId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public String reference(Integer missionId) {
        return missionRepo.findById(missionId).map(Mission::getReference).orElse("mission");
    }

    // ---- helpers ----

    private AgentOption option(Agent a) {
        return new AgentOption(a.getMatricule(), a.getMatricule() + " — " + a.getNom() + " " + a.getPrenom());
    }

    private Agent resoudreChef(String sel, String mat, String nom, String prenom, TypeAgent type, Poste poste, String role) {
        if (sel != null && !sel.isBlank()) {
            return agentRepo.findById(sel.trim()).orElseThrow();
        }
        final String fm = (mat == null) ? "" : mat.trim();
        if (fm.isEmpty()) {
            throw new IllegalArgumentException("Choisissez un " + role + " existant ou renseignez le matricule du nouvel agent.");
        }
        final String fnom = nom, fprenom = prenom;
        return agentRepo.findById(fm).orElseGet(() -> {
            Agent a = new Agent();
            a.setMatricule(fm);
            a.setNom((fnom == null || fnom.isBlank()) ? fm : fnom.trim());
            a.setPrenom((fprenom == null || fprenom.isBlank()) ? "-" : fprenom.trim());
            a.setTypeAgent(type);
            a.setPoste(type == TypeAgent.POSTE ? poste : null);
            return agentRepo.save(a);
        });
    }

    /** Maintient l'historique du chef de poste : reconduit si inchangé, sinon clôture l'ancien et ouvre le nouveau. */
    private void historiserChefPoste(Poste poste, Agent chef, LocalDate debut) {
        Optional<ChefPoste> courant = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(poste.getId());
        if (courant.isPresent()) {
            if (courant.get().getAgent().getMatricule().equals(chef.getMatricule())) return; // reconduit
            courant.get().setDateFin(debut);
            chefPosteRepo.saveAndFlush(courant.get());
        }
        ChefPoste cp = new ChefPoste();
        cp.setPoste(poste);
        cp.setAgent(chef);
        cp.setDateDebut(debut);
        chefPosteRepo.save(cp);
    }

    private String genererReference() {
        String prefixe = "MIS-" + Year.now().getValue() + "-";
        long n = missionRepo.countByReferenceStartingWith(prefixe) + 1;
        return prefixe + String.format("%03d", n);
    }

    private LocalDate parseDate(String s, LocalDate defaut) {
        if (s == null || s.isBlank()) return defaut;
        String v = s.trim();
        for (DateTimeFormatter f : FORMATS) {
            try { return LocalDate.parse(v, f); } catch (DateTimeParseException ignored) { }
        }
        return defaut;
    }
}
