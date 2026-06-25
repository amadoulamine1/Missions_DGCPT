package sn.dgcpt.missionsparc.importation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.domain.LotImport;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutLot;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;
import sn.dgcpt.missionsparc.importation.dto.LigneOrdinateur;
import sn.dgcpt.missionsparc.repository.LotImportRepository;
import sn.dgcpt.missionsparc.repository.MissionRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Logique de consolidation multi-fichiers (§5.1 / §9.9) : détection des conflits par clé
 * (n° d'inventaire, sinon MAC / n° de série), arbitrage du chef de mission et intégration.
 *
 * Les collaborateurs concrets ({@link CanevasReader}, {@link IntegrationService}) sont
 * remplacés par des doublures manuelles (sous-classes) plutôt que par des mocks Mockito :
 * sous JDK récent, l'instrumentation « inline » des classes concrètes échoue, alors que
 * les dépôts (interfaces) restent mockables. Chaque lot porte un octet « marqueur » qui
 * désigne le {@link CanevasImporte} en mémoire à renvoyer, ce qui évite de fabriquer un
 * vrai fichier .xlsx.
 */
@ExtendWith(MockitoExtension.class)
class ConsolidationServiceTest {

    private static final Integer MISSION_ID = 10;

    @Mock MissionRepository missionRepo;
    @Mock LotImportRepository lotRepo;

    private FakeIntegration integration;
    private ConsolidationService service;

    private void init(Map<Integer, CanevasImporte> parMarqueur) {
        integration = new FakeIntegration();
        service = new ConsolidationService(integration, missionRepo, lotRepo, new FakeReader(parMarqueur));
    }

    // ---------- doublures manuelles ----------

    /** Renvoie le canevas associé à l'octet marqueur du flux lu (lot.getFichier()[0]). */
    private static final class FakeReader extends CanevasReader {
        private final Map<Integer, CanevasImporte> parMarqueur;
        FakeReader(Map<Integer, CanevasImporte> parMarqueur) { this.parMarqueur = parMarqueur; }
        @Override public CanevasImporte lire(InputStream is) throws IOException {
            int marqueur = is.read();
            CanevasImporte cv = parMarqueur.get(marqueur);
            if (cv == null) throw new IOException("marqueur de lot inconnu : " + marqueur);
            return cv;
        }
    }

    /** Compte les intégrations et renvoie 1 (une ligne intégrée) par appel. */
    private static final class FakeIntegration extends IntegrationService {
        int appels = 0;
        FakeIntegration() { super(null, null, null, null, null, null, null, null, null, null, null, null); }
        @Override public int integrer(CanevasImporte canevas) { appels++; return 1; }
    }

    // ---------- fabriques ----------

    private CanevasImporte canevasOrd(String numero, String mac, String nomMachine) {
        CanevasImporte cv = new CanevasImporte();
        LigneOrdinateur o = new LigneOrdinateur();
        o.setNumeroInventaire(numero);
        o.setMacEthernet(mac);
        o.setNomMachine(nomMachine);
        cv.getOrdinateurs().add(o);
        return cv;
    }

    private LotImport lot(int id, int marqueur, String agent) {
        LotImport l = new LotImport();
        l.setId(id);
        l.setFichier(new byte[]{(byte) marqueur});
        l.setAgentSaisisseur(agent);
        l.setSourceFichier("fichier-" + id + ".xlsx");
        l.setStatut(StatutLot.EN_ATTENTE);
        Mission m = new Mission(); m.setId(MISSION_ID);
        l.setMission(m);
        return l;
    }

    private void enAttente(LotImport... lots) {
        when(lotRepo.findByMission_IdAndStatut(MISSION_ID, StatutLot.EN_ATTENTE)).thenReturn(List.of(lots));
    }

    // ---------- détection des conflits ----------

    @Test
    void conflits_signale_un_conflit_quand_le_meme_numero_porte_des_valeurs_divergentes() {
        enAttente(lot(1, 1, "AG1"), lot(2, 2, "AG2"));
        init(Map.of(
                1, canevasOrd("ORD-1", null, "PC-A"),
                2, canevasOrd("ORD-1", null, "PC-B")));

        List<Conflit> conflits = service.conflits(MISSION_ID);

        assertThat(conflits).hasSize(1);
        Conflit c = conflits.get(0);
        assertThat(c.getCle()).isEqualTo("N:ORD-1");
        assertThat(c.getType()).isEqualTo("Ordinateur");
        assertThat(c.getOptions()).extracting(OptionConflit::getLotId).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void conflits_aucun_conflit_quand_les_valeurs_sont_identiques() {
        enAttente(lot(1, 1, "AG1"), lot(2, 2, "AG2"));
        init(Map.of(
                1, canevasOrd("ORD-1", null, "PC-A"),
                2, canevasOrd("ORD-1", null, "PC-A")));

        assertThat(service.conflits(MISSION_ID)).isEmpty();
    }

    @Test
    void conflits_aucun_conflit_quand_la_cle_n_apparait_que_dans_un_seul_lot() {
        enAttente(lot(1, 1, "AG1"), lot(2, 2, "AG2"));
        init(Map.of(
                1, canevasOrd("ORD-1", null, "PC-A"),
                2, canevasOrd("ORD-2", null, "PC-B")));

        assertThat(service.conflits(MISSION_ID)).isEmpty();
    }

    @Test
    void conflits_utilise_la_mac_comme_cle_de_repli_quand_le_numero_est_absent() {
        enAttente(lot(1, 1, "AG1"), lot(2, 2, "AG2"));
        init(Map.of(
                1, canevasOrd(null, "AA:BB:CC:DD:EE:FF", "PC-A"),
                2, canevasOrd("", "AA:BB:CC:DD:EE:FF", "PC-B")));

        List<Conflit> conflits = service.conflits(MISSION_ID);

        assertThat(conflits).hasSize(1);
        assertThat(conflits.get(0).getCle()).isEqualTo("K:aa:bb:cc:dd:ee:ff");
    }

    // ---------- intégration / arbitrage ----------

    @Test
    void integrer_ne_conserve_que_le_lot_gagnant_pour_la_cle_en_conflit() {
        CanevasImporte cv1 = canevasOrd("ORD-1", null, "PC-A");
        CanevasImporte cv2 = canevasOrd("ORD-1", null, "PC-B");
        enAttente(lot(1, 1, "AG1"), lot(2, 2, "AG2"));
        init(Map.of(1, cv1, 2, cv2));

        Map<String, Integer> arbitrages = new HashMap<>();
        arbitrages.put("N:ORD-1", 2); // le chef retient la version du lot #2

        int total = service.integrer(MISSION_ID, arbitrages);

        assertThat(total).isEqualTo(2);
        assertThat(cv1.getOrdinateurs()).isEmpty();     // perdant : ligne en conflit retirée
        assertThat(cv2.getOrdinateurs()).hasSize(1);    // gagnant : ligne conservée
    }

    @Test
    void integrer_retient_le_premier_lot_par_defaut_en_l_absence_d_arbitrage() {
        CanevasImporte cv1 = canevasOrd("ORD-1", null, "PC-A");
        CanevasImporte cv2 = canevasOrd("ORD-1", null, "PC-B");
        enAttente(lot(1, 1, "AG1"), lot(2, 2, "AG2"));
        init(Map.of(1, cv1, 2, cv2));

        service.integrer(MISSION_ID, null);

        assertThat(cv1.getOrdinateurs()).hasSize(1);    // premier lot conservé par défaut
        assertThat(cv2.getOrdinateurs()).isEmpty();
    }

    @Test
    void integrer_marque_les_lots_comme_integres() {
        LotImport l1 = lot(1, 1, "AG1");
        LotImport l2 = lot(2, 2, "AG2");
        enAttente(l1, l2);
        init(Map.of(
                1, canevasOrd("ORD-1", null, "PC-A"),
                2, canevasOrd("ORD-2", null, "PC-B")));

        int total = service.integrer(MISSION_ID, null);

        assertThat(total).isEqualTo(2);
        assertThat(integration.appels).isEqualTo(2);
        assertThat(l1.getStatut()).isEqualTo(StatutLot.INTEGRE);
        assertThat(l2.getStatut()).isEqualTo(StatutLot.INTEGRE);
        verify(lotRepo).save(l1);
        verify(lotRepo).save(l2);
    }

    @Test
    void integrer_sans_lot_en_attente_leve_une_exception() {
        when(lotRepo.findByMission_IdAndStatut(MISSION_ID, StatutLot.EN_ATTENTE)).thenReturn(List.of());
        init(Map.of());

        assertThatThrownBy(() -> service.integrer(MISSION_ID, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun fichier");

        assertThat(integration.appels).isZero();
    }

    // ---------- création / suppression de lot ----------

    @Test
    void creerLot_rattache_le_fichier_a_la_mission_par_sa_reference() {
        init(Map.of());
        CanevasImporte cv = new CanevasImporte();
        cv.getEntete().setReference("MIS-2026-001");
        cv.getEntete().setAgentSaisisseur("AG1");
        Mission mission = new Mission(); mission.setId(MISSION_ID); mission.setReference("MIS-2026-001");
        when(missionRepo.findByReference("MIS-2026-001")).thenReturn(Optional.of(mission));

        Integer missionId = service.creerLot(cv, new byte[]{9}, "releve.xlsx");

        assertThat(missionId).isEqualTo(MISSION_ID);
        ArgumentCaptor<LotImport> captor = ArgumentCaptor.forClass(LotImport.class);
        verify(lotRepo).save(captor.capture());
        LotImport sauve = captor.getValue();
        assertThat(sauve.getMission()).isEqualTo(mission);
        assertThat(sauve.getAgentSaisisseur()).isEqualTo("AG1");
        assertThat(sauve.getStatut()).isEqualTo(StatutLot.EN_ATTENTE);
        assertThat(sauve.getSourceFichier()).isEqualTo("releve.xlsx");
    }

    @Test
    void creerLot_reference_inconnue_leve_une_exception() {
        init(Map.of());
        CanevasImporte cv = new CanevasImporte();
        cv.getEntete().setReference("MIS-INCONNUE");
        when(missionRepo.findByReference("MIS-INCONNUE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerLot(cv, new byte[]{9}, "x.xlsx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");

        verify(lotRepo, never()).save(any());
    }

    @Test
    void supprimerLot_supprime_le_lot_et_retourne_l_id_de_la_mission() {
        init(Map.of());
        LotImport l = lot(5, 1, "AG1");
        when(lotRepo.findById(5)).thenReturn(Optional.of(l));

        Integer missionId = service.supprimerLot(5);

        assertThat(missionId).isEqualTo(MISSION_ID);
        verify(lotRepo).delete(l);
    }
}
