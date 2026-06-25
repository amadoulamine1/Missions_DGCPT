package sn.dgcpt.missionsparc.importation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;
import sn.dgcpt.missionsparc.importation.dto.LigneOrdinateur;
import sn.dgcpt.missionsparc.repository.*;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le canevas d'exemple sans base ni Docker (le test d'intégration complet,
 * {@link ImportIntegrationTest}, ne s'exécute qu'en CI). Couvre la lecture de la colonne
 * « AD » (logiciel figé) et l'alignement des colonnes de queue de l'onglet « 3-Ordinateurs »,
 * puis l'intégrabilité du canevas via {@link ControleImport} (dépôts mockés).
 */
@ExtendWith(MockitoExtension.class)
class CanevasFixtureTest {

    @Mock OrdinateurRepository ordinateurRepo;
    @Mock ImprimanteRepository imprimanteRepo;
    @Mock EquipementReseauRepository reseauRepo;
    @Mock ScannerChequeRepository scannerRepo;
    @Mock MaterielRepository materielRepo;
    @Mock CategorieMaterielRepository categorieMaterielRepo;

    private CanevasImporte lireFixture() throws Exception {
        try (InputStream is = new ClassPathResource("canevas-exemple-rempli.xlsx").getInputStream()) {
            return new CanevasReader().lire(is);
        }
    }

    @Test
    void lit_la_colonne_AD_et_aligne_les_colonnes_de_queue() throws Exception {
        CanevasImporte c = lireFixture();
        assertThat(c.getOrdinateurs()).hasSize(2);

        LigneOrdinateur o1 = c.getOrdinateurs().get(0);
        assertThat(o1.getNomMachine()).isEqualTo("PC-COMPTA-01");
        assertThat(o1.isAd()).as("AD = Oui sur la 1re ligne").isTrue();
        // le statut, lu après la nouvelle colonne AD, reste correctement aligné
        assertThat(o1.getStatut()).isEqualTo("En service");

        assertThat(c.getOrdinateurs().get(1).isAd()).as("AD = Non sur la 2e ligne").isFalse();
    }

    @Test
    void le_canevas_d_exemple_est_integrable() throws Exception {
        CanevasImporte c = lireFixture();
        ControleImport controle = new ControleImport(ordinateurRepo, imprimanteRepo, reseauRepo,
                scannerRepo, materielRepo, categorieMaterielRepo);

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable())
                .as("aucune anomalie bloquante attendue, trouvé : %s", r.getAnomalies())
                .isTrue();
    }
}
