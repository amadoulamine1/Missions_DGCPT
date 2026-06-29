package sn.dgcpt.missionsparc.mission;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Génération du canevas (Apache POI) — couvre le code le plus complexe en écriture :
 * le canevas pré-estampillé doit s'ouvrir, porter l'en-tête de la mission, marquer le chef de poste
 * comme obligatoire (astérisque) et protéger les feuilles de matériel (verrouillage du N° d'inventaire,
 * dont l'onglet « 7-Autres matériels » désormais protégé).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CanevasWriterTest {

    @Mock AgentRepository agentRepo;
    @Mock MaterielRepository materielRepo;
    @Mock OrdinateurRepository ordinateurRepo;
    @Mock ImprimanteRepository imprimanteRepo;
    @Mock EquipementReseauRepository reseauRepo;
    @Mock ScannerChequeRepository scannerRepo;
    @Mock AffectationMaterielRepository affectationRepo;
    @Mock CategorieMaterielRepository categorieMaterielRepo;

    private CanevasWriter writer;

    @BeforeEach
    void setUp() {
        writer = new CanevasWriter(agentRepo, materielRepo, ordinateurRepo, imprimanteRepo,
                reseauRepo, scannerRepo, affectationRepo, categorieMaterielRepo);
        when(materielRepo.findByPoste_Id(anyInt())).thenReturn(List.of());
        when(agentRepo.findByPoste_Id(anyInt())).thenReturn(List.of());
        when(categorieMaterielRepo.findByFamilleAndActifTrueOrderByLibelle(any())).thenReturn(List.of());
    }

    @Test
    void prestampe_un_canevas_valide_avec_entete_chef_de_poste_obligatoire_et_feuilles_protegees() throws Exception {
        Mission m = mission();

        byte[] bytes = writer.prestamper(m, m.getChefMission());

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet entete = wb.getSheet("1-Mission et Réseau");
            assertThat(entete).as("feuille d'en-tête présente").isNotNull();

            // En-tête pré-estampillée
            assertThat(valeur(entete, "n° de mission")).isEqualTo("MIS-2026-001");
            assertThat(valeur(entete, "code poste")).isEqualTo("DKR");

            // Chef de poste : marqué obligatoire (astérisque) et pré-rempli
            assertThat(libelle(entete, "chef de poste")).contains("*");
            assertThat(valeur(entete, "chef de poste")).contains("AGP1");

            // Verrouillage du N° d'inventaire : feuilles de matériel protégées,
            // y compris « 7-Autres matériels » (protégée par programme).
            assertThat(wb.getSheet("3-Ordinateurs").getProtect()).as("3-Ordinateurs protégée").isTrue();
            assertThat(wb.getSheet("7-Autres matériels").getProtect()).as("7-Autres protégée").isTrue();

            // Colonne « AD » (M = index 12) réparée : cellule de saisie déverrouillée (sinon Excel
            // refuse de modifier Oui/Non sur la feuille protégée).
            org.apache.poi.ss.usermodel.Cell ad =
                    wb.getSheet("3-Ordinateurs").getRow(1).getCell(12); // ligne 2, colonne M
            assertThat(ad).as("cellule AD (M2) matérialisée").isNotNull();
            assertThat(ad.getCellStyle().getLocked()).as("AD déverrouillée").isFalse();

            // « Ligne saisie » qui inclut AD (jusqu'à R) : renseigner AD rend les obligatoires requis (surlignés).
            assertThat(mefcInclutAD(wb.getSheet("3-Ordinateurs")))
                    .as("une règle de mise en forme conditionnelle déclenche sur $A2:$R2 (AD inclus)").isTrue();
        }
    }

    /** Vrai si une règle de MFC de la feuille utilise la plage « ligne saisie » étendue à la colonne R (AD). */
    private boolean mefcInclutAD(Sheet s) {
        var scf = s.getSheetConditionalFormatting();
        for (int i = 0; i < scf.getNumConditionalFormattings(); i++) {
            var cf = scf.getConditionalFormattingAt(i);
            for (int j = 0; j < cf.getNumberOfRules(); j++) {
                String f = cf.getRule(j).getFormula1();
                if (f != null && f.contains("$A2:$R2")) return true;
            }
        }
        return false;
    }

    // ---------- fabriques & helpers ----------

    private Mission mission() {
        Poste p = new Poste();
        p.setId(1); p.setCode("DKR"); p.setNom("Dakar"); p.setRegion("Dakar");

        Agent chefMission = agent("IN001", "Diop", "Awa", TypeAgent.INFORMATICIEN);
        Agent chefPoste = agent("AGP1", "Sow", "Modou", TypeAgent.POSTE);

        Mission m = new Mission();
        m.setReference("MIS-2026-001");
        m.setObjet("Inventaire");
        m.setDateDebut(LocalDate.of(2026, 6, 1));
        m.setDateFin(LocalDate.of(2026, 6, 5));
        m.setPoste(p);
        m.setChefMission(chefMission);
        m.setChefPosteFige(chefPoste);
        m.setMembres(Set.of(chefMission));
        return m;
    }

    private Agent agent(String mat, String nom, String prenom, TypeAgent type) {
        Agent a = new Agent();
        a.setMatricule(mat); a.setNom(nom); a.setPrenom(prenom); a.setTypeAgent(type);
        return a;
    }

    /** Valeur (colonne B) de la ligne dont le libellé (colonne A) commence par {@code prefixe}. */
    private String valeur(Sheet s, String prefixe) {
        int r = ligneLabel(s, prefixe);
        if (r < 0) return null;
        Cell b = s.getRow(r).getCell(1);
        return (b == null || b.getCellType() != CellType.STRING) ? "" : b.getStringCellValue();
    }

    /** Libellé brut (colonne A) de la ligne portant le préfixe. */
    private String libelle(Sheet s, String prefixe) {
        int r = ligneLabel(s, prefixe);
        return r < 0 ? null : s.getRow(r).getCell(0).getStringCellValue();
    }

    private int ligneLabel(Sheet s, String prefixe) {
        for (Row row : s) {
            Cell a = row.getCell(0);
            if (a == null || a.getCellType() != CellType.STRING) continue;
            String lab = a.getStringCellValue().replace("*", "").trim().toLowerCase();
            if (lab.startsWith(prefixe)) return row.getRowNum();
        }
        return -1;
    }
}
