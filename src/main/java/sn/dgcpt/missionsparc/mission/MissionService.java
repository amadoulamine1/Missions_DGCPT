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
import java.util.ArrayList;
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

        List<String> membres = (f.getMembres() == null) ? List.of()
                : f.getMembres().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().collect(Collectors.toList());
        if (membres.isEmpty()) {
            throw new IllegalArgumentException("Sélectionnez au moins un membre pour la mission.");
        }

        String chefMat = (f.getChefMissionSel() == null) ? "" : f.getChefMissionSel().trim();
        if (chefMat.isEmpty() || !membres.contains(chefMat)) {
            throw new IllegalArgumentException("Désignez le chef de mission parmi les membres sélectionnés (un seul).");
        }

        verifierChevauchement(membres, debut, fin);

        Poste poste = (f.getPosteId() != null)
                ? posteRepo.findById(f.getPosteId()).orElseThrow()
                : referentiel.resoudrePoste(f.getCodePoste(), f.getNomPoste());

        Agent chefMission = agentRepo.findById(chefMat).orElseThrow();
        Agent chefPoste = resoudreChefPoste(f, poste);

        Mission m = new Mission();
        m.setReference(genererReference());
        m.setObjet((f.getObjet() == null || f.getObjet().isBlank()) ? "(mission)" : f.getObjet().trim());
        m.setDateDebut(debut);
        m.setDateFin(fin);
        m.setPoste(poste);
        m.setChefMission(chefMission);
        m.setChefPosteFige(chefPoste);
        m.setStatut(StatutMission.EN_CONSOLIDATION);
        for (String mat : membres) {
            agentRepo.findById(mat).ifPresent(m.getMembres()::add);
        }
        Mission saved = missionRepo.save(m);

        historiserChefPoste(poste, chefPoste, debut);
        return saved;
    }

    private void verifierChevauchement(List<String> membres, LocalDate debut, LocalDate fin) {
        LocalDate finEff = (fin != null) ? fin : LocalDate.of(9999, 12, 31);
        List<String> conflits = new ArrayList<>();
        for (String mat : membres) {
            List<Mission> c = missionRepo.membreEnConflit(mat, debut, finEff);
            if (!c.isEmpty()) {
                conflits.add(mat + " (déjà sur " + c.get(0).getReference() + ")");
            }
        }
        if (!conflits.isEmpty()) {
            throw new IllegalArgumentException(
                    "Chevauchement de période — ces agents sont déjà membres d'une mission sur la même période : "
                    + String.join(" ; ", conflits)
                    + ". Retirez-les d'abord de leur mission existante avant de les ajouter ici.");
        }
    }

    @Transactional
    public void retirerMembre(Integer missionId, String matricule) {
        Mission m = missionRepo.findById(missionId).orElseThrow();
        m.getMembres().removeIf(a -> a.getMatricule().equals(matricule));
        missionRepo.save(m);
    }

    @Transactional(readOnly = true)
    public List<AgentOption> informaticiens() {
        return agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN).stream().map(this::option).collect(Collectors.toList());
    }

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

    private AgentOption option(Agent a) {
        return new AgentOption(a.getMatricule(), a.getMatricule() + " — " + a.getNom() + " " + a.getPrenom());
    }

    private Agent resoudreChefPoste(CreationMissionForm f, Poste poste) {
        if (f.getChefPosteSel() != null && !f.getChefPosteSel().isBlank()) {
            return agentRepo.findById(f.getChefPosteSel().trim()).orElseThrow();
        }
        final String fm = (f.getChefPosteMat() == null) ? "" : f.getChefPosteMat().trim();
        if (fm.isEmpty()) {
            throw new IllegalArgumentException("Choisissez un chef de poste existant ou renseignez le matricule du nouvel agent.");
        }
        final String fnom = f.getChefPosteNom(), fprenom = f.getChefPostePrenom();
        return agentRepo.findById(fm).orElseGet(() -> {
            Agent a = new Agent();
            a.setMatricule(fm);
            a.setNom((fnom == null || fnom.isBlank()) ? fm : fnom.trim());
            a.setPrenom((fprenom == null || fprenom.isBlank()) ? "-" : fprenom.trim());
            a.setTypeAgent(TypeAgent.POSTE);
            a.setPoste(poste);
            return agentRepo.save(a);
        });
    }

    private void historiserChefPoste(Poste poste, Agent chef, LocalDate debut) {
        Optional<ChefPoste> courant = chefPosteRepo.findFirstByPoste_IdAndDateFinIsNull(poste.getId());
        if (courant.isPresent()) {
            if (courant.get().getAgent().getMatricule().equals(chef.getMatricule())) return;
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
