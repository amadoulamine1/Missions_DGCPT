package sn.dgcpt.missionsparc.importation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutMission;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;
import sn.dgcpt.missionsparc.repository.*;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ImportIntegrationTest {

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

        // matériel et sous-types
        assertThat(materielRepo.count()).isEqualTo(5);
        assertThat(ordinateurRepo.count()).isEqualTo(2);
        assertThat(imprimanteRepo.count()).isEqualTo(1);
        assertThat(reseauRepo.count()).isEqualTo(1);
        assertThat(scannerRepo.count()).isEqualTo(1);

        // relevés (photo datée) et affectations (un par matériel)
        assertThat(releveRepo.count()).isEqualTo(5);
        assertThat(affectationRepo.count()).isEqualTo(5);

        // mission créée, en consolidation
        Mission mission = missionRepo.findByReference("MIS-2026-001").orElseThrow();
        assertThat(mission.getStatut()).isEqualTo(StatutMission.EN_CONSOLIDATION);

        // numéros d'inventaire générés pour les nouveaux ordinateurs
        assertThat(ordinateurRepo.findAll())
                .allSatisfy(o -> assertThat(o.getNumeroInventaire()).startsWith("DKR-PC-"));

        // agents résolus / créés à la volée
        assertThat(agentRepo.findById("AG001")).isPresent();
        assertThat(agentRepo.findById("IN001")).isPresent();

        long materiels = materielRepo.count();
        long releves = releveRepo.count();
        long affectations = affectationRepo.count();

        // second import du même fichier : rapprochement par MAC / n° de série -> aucun doublon
        importService.integrer(lireExemple());
        assertThat(materielRepo.count()).as("pas de doublon de matériel").isEqualTo(materiels);
        assertThat(releveRepo.count()).as("relevés mis à jour, pas dupliqués").isEqualTo(releves);
        assertThat(affectationRepo.count()).as("affectations inchangées").isEqualTo(affectations);
    }
}
