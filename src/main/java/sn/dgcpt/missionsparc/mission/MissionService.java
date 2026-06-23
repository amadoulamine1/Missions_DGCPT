package sn.dgcpt.missionsparc.mission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.StatutMission;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.MissionRepository;
import sn.dgcpt.missionsparc.repository.PosteRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class MissionService {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private final MissionRepository missionRepo;
    private final PosteRepository posteRepo;
    private final ReferentielService referentiel;
    private final CanevasWriter canevasWriter;

    public MissionService(MissionRepository missionRepo, PosteRepository posteRepo,
                          ReferentielService referentiel, CanevasWriter canevasWriter) {
        this.missionRepo = missionRepo;
        this.posteRepo = posteRepo;
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

        Mission m = new Mission();
        m.setReference(genererReference());
        m.setObjet((f.getObjet() == null || f.getObjet().isBlank()) ? "(mission)" : f.getObjet().trim());
        m.setDateDebut(debut);
        m.setDateFin(fin);
        m.setPoste(poste);
        m.setChefMission(referentiel.resoudreAgent(f.getChefMission(), TypeAgent.INFORMATICIEN, null));
        m.setChefPosteFige(referentiel.resoudreAgent(f.getChefPoste(), TypeAgent.POSTE, poste));
        m.setStatut(StatutMission.EN_CONSOLIDATION);
        return missionRepo.save(m);
    }

    @Transactional(readOnly = true)
    public byte[] genererCanevas(Integer missionId) throws IOException {
        Mission m = missionRepo.findById(missionId).orElseThrow();
        return canevasWriter.prestamper(m);
    }

    @Transactional(readOnly = true)
    public String reference(Integer missionId) {
        return missionRepo.findById(missionId).map(Mission::getReference).orElse("mission");
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
