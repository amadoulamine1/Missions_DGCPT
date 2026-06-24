package sn.dgcpt.missionsparc.importation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.LotImport;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutLot;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;
import sn.dgcpt.missionsparc.repository.LotImportRepository;
import sn.dgcpt.missionsparc.repository.MissionRepository;

import java.time.Instant;

/** Consolidation des fichiers (lots) d'une mission, jusqu'à l'intégration arbitrée. */
@Service
public class ConsolidationService {

    private final IntegrationService integration;
    private final MissionRepository missionRepo;
    private final LotImportRepository lotRepo;
    private final CanevasReader reader;

    public ConsolidationService(IntegrationService integration, MissionRepository missionRepo,
                                LotImportRepository lotRepo, CanevasReader reader) {
        this.integration = integration;
        this.missionRepo = missionRepo;
        this.lotRepo = lotRepo;
        this.reader = reader;
    }

    /** Crée un lot en attente pour la mission référencée par le canevas. */
    @Transactional
    public Integer creerLot(CanevasImporte cv, byte[] bytes, String filename) {
        String ref = (cv.getEntete().getReference() == null) ? "" : cv.getEntete().getReference().trim();
        Mission mission = missionRepo.findByReference(ref).orElseThrow(() ->
                new IllegalArgumentException("Mission « " + ref + " » introuvable. Créez la mission avant d'importer ce fichier."));
        LotImport lot = new LotImport();
        lot.setMission(mission);
        lot.setAgentSaisisseur(cv.getEntete().getAgentSaisisseur());
        lot.setSourceFichier(filename);
        lot.setDateChargement(Instant.now());
        lot.setFichier(bytes);
        lot.setStatut(StatutLot.EN_ATTENTE);
        lotRepo.save(lot);
        return mission.getId();
    }
}
