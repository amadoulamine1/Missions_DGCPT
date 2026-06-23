package sn.dgcpt.missionsparc.importation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutMission;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;
import sn.dgcpt.missionsparc.repository.*;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test du pipeline d'import contre une vraie PostgreSQL (Testcontainers).
 * Ignoré automatiquement si Docker n'est pas disponible (démarrez Docker Desktop pour l'exécuter).
 */
@SpringBootTest
@Testcontainers
@EnabledIf("dockerDisponible")
class ImportIntegrationTest {

    static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired ImportService importService;
    @Autowired MaterielRepository materielRepo;
    @Autowired OrdinateurRepository ordinateurRepo;
    @Autowired ImprimanteRepository imprimanteRepo;
    @Autowired EquipementReseauRepository reseauRepo;
    @Autowired ScannerChequeRepository scannerRepo;
    @Autowired MissionRepository missionRepo;
    @Autowired ReleveMaterielRepository releveRepo;
    @Autowired AffectationMaterielRepository affectationRepo;
    @Autowired AgentRepository agentRepo;

    private CanevasImporte lireExemple() throws Exception {
        try (InputStream is = new ClassPathResource("canevas-exemple-rempli.xlsx").getInputStream()) {
            return importService.lire(is);
        }
    }

    @Test
    void importe_consolide_et_est_idempotent() throws Exception {
        CanevasImporte canevas = lireExemple();

        RapportImport rapport = importService.controler(canevas);
        assertThat(rapport.estIntegrable())
                .as("aucune anomalie bloquante attendue")
                .isTrue();

        int nb = importService.integrer(canevas);
        assertThat(nb).isEqualTo(5);

        assertThat(materielRepo.count()).isEqualTo(5);
        assertThat(ordinateurRepo.count()).isEqualTo(2);
        assertThat(imprimanteRepo.count()).isEqualTo(1);
        assertThat(reseauRepo.count()).isEqualTo(1);
        assertThat(scannerRepo.count()).isEqualTo(1);

        assertThat(releveRepo.count()).isEqualTo(5);
        assertThat(affectationRepo.count()).isEqualTo(5);

        Mission mission = missionRepo.findByReference("MIS-2026-001").orElseThrow();
        assertThat(mission.getStatut()).isEqualTo(StatutMission.EN_CONSOLIDATION);

        assertThat(ordinateurRepo.findAll())
                .allSatisfy(o -> assertThat(o.getNumeroInventaire()).startsWith("DKR-PC-"));

        assertThat(agentRepo.findById("AG001")).isPresent();
        assertThat(agentRepo.findById("IN001")).isPresent();

        long materiels = materielRepo.count();
        long releves = releveRepo.count();
        long affectations = affectationRepo.count();

        importService.integrer(lireExemple());
        assertThat(materielRepo.count()).as("pas de doublon de matériel").isEqualTo(materiels);
        assertThat(releveRepo.count()).as("relevés mis à jour, pas dupliqués").isEqualTo(releves);
        assertThat(affectationRepo.count()).as("affectations inchangées").isEqualTo(affectations);
    }
}
