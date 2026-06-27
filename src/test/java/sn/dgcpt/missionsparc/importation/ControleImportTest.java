package sn.dgcpt.missionsparc.importation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.dgcpt.missionsparc.domain.CategorieMateriel;
import sn.dgcpt.missionsparc.domain.Materiel;
import sn.dgcpt.missionsparc.domain.Ordinateur;
import sn.dgcpt.missionsparc.domain.ScannerCheque;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;
import sn.dgcpt.missionsparc.importation.dto.EnteteMission;
import sn.dgcpt.missionsparc.importation.dto.LigneAutreMateriel;
import sn.dgcpt.missionsparc.importation.dto.LigneOrdinateur;
import sn.dgcpt.missionsparc.importation.dto.LigneScanner;
import sn.dgcpt.missionsparc.repository.CategorieMaterielRepository;
import sn.dgcpt.missionsparc.repository.EquipementReseauRepository;
import sn.dgcpt.missionsparc.repository.ImprimanteRepository;
import sn.dgcpt.missionsparc.repository.MaterielRepository;
import sn.dgcpt.missionsparc.repository.OrdinateurRepository;
import sn.dgcpt.missionsparc.repository.ScannerChequeRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Contrôles à l'import (§5) et filet de sécurité anti-doublon (§4) : une ligne sans
 * n° d'inventaire dont la MAC / le n° de série est déjà connu en base est signalée
 * en AVERTISSEMENT (non bloquant), tandis que MAC absente ou mal formée et champs
 * d'en-tête manquants sont BLOQUANTS.
 */
@ExtendWith(MockitoExtension.class)
class ControleImportTest {

    @Mock OrdinateurRepository ordinateurRepo;
    @Mock ImprimanteRepository imprimanteRepo;
    @Mock EquipementReseauRepository reseauRepo;
    @Mock ScannerChequeRepository scannerRepo;
    @Mock MaterielRepository materielRepo;
    @Mock CategorieMaterielRepository categorieMaterielRepo;
    @InjectMocks ControleImport controle;

    private static final String MAC_VALIDE = "AA:BB:CC:DD:EE:FF";

    private void enteteValide(CanevasImporte c) {
        EnteteMission e = c.getEntete();
        e.setReference("MIS-2026-001");
        e.setCodePoste("TPR-01");
        e.setObjet("Inventaire");
        e.setAgentSaisisseur("AG1");
        e.setChefMission("AG1");
        e.setChefPoste("CP1");
        e.setEtatCablage("Bon");
        e.setCategorieCable("Cat6");
    }

    private LigneOrdinateur ordinateurValide(String numero, String mac) {
        LigneOrdinateur o = new LigneOrdinateur();
        o.setNumLigne(2);
        o.setNumeroInventaire(numero);
        o.setNomMachine("PC-A");
        o.setAgentAttributaire("A1");
        o.setAgentInstallateur("A2");
        o.setMacEthernet(mac);
        o.setStatut("En service"); // le statut est désormais obligatoire
        return o;
    }

    private boolean contient(RapportImport r, Severite sev, String fragment) {
        return r.getAnomalies().stream()
                .anyMatch(a -> a.getSeverite() == sev && a.getMessage().contains(fragment));
    }

    // ---------- filet anti-doublon (§4) ----------

    @Test
    void avertissement_quand_mac_deja_connue_sans_numero_inventaire() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getOrdinateurs().add(ordinateurValide(null, MAC_VALIDE));
        when(ordinateurRepo.findByMacEthernet(MAC_VALIDE)).thenReturn(Optional.of(new Ordinateur()));

        RapportImport r = controle.controler(c);

        assertThat(r.nbAvertissements()).isEqualTo(1);
        assertThat(r.estIntegrable()).isTrue(); // un avertissement ne bloque pas l'intégration
        assertThat(contient(r, Severite.AVERTISSEMENT, "déjà connu")).isTrue();
    }

    @Test
    void pas_d_avertissement_quand_le_numero_d_inventaire_est_present() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getOrdinateurs().add(ordinateurValide("ORD-1", MAC_VALIDE)); // matériel déjà identifié
        // findByMacEthernet ne doit pas être consulté : la ligne n'est pas « nouvelle »

        RapportImport r = controle.controler(c);

        assertThat(r.nbAvertissements()).isZero();
        assertThat(r.estIntegrable()).isTrue();
    }

    @Test
    void pas_d_avertissement_quand_la_mac_est_inconnue_en_base() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getOrdinateurs().add(ordinateurValide(null, MAC_VALIDE));
        when(ordinateurRepo.findByMacEthernet(MAC_VALIDE)).thenReturn(Optional.empty());

        RapportImport r = controle.controler(c);

        assertThat(r.nbAvertissements()).isZero();
    }

    @Test
    void avertissement_doublon_scanner_par_numero_de_serie() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        LigneScanner sc = new LigneScanner();
        sc.setNumLigne(2);
        sc.setNumeroInventaire(null);
        sc.setNumeroSerie("SN-123");
        c.getScanners().add(sc);
        when(scannerRepo.findByNumeroSerie("SN-123")).thenReturn(Optional.of(new ScannerCheque()));

        RapportImport r = controle.controler(c);

        assertThat(r.nbAvertissements()).isEqualTo(1);
        assertThat(contient(r, Severite.AVERTISSEMENT, "n° de série SN-123")).isTrue();
    }

    // ---------- contrôles bloquants ----------

    @Test
    void mac_ethernet_manquante_est_bloquante() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getOrdinateurs().add(ordinateurValide("ORD-1", "")); // MAC absente

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        assertThat(contient(r, Severite.BLOQUANT, "MAC ethernet")).isTrue();
    }

    @Test
    void mac_au_format_invalide_est_bloquante() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getOrdinateurs().add(ordinateurValide("ORD-1", "ZZ:00")); // format invalide

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        assertThat(contient(r, Severite.BLOQUANT, "Format d'adresse MAC invalide")).isTrue();
    }

    @Test
    void entete_incomplete_est_bloquante() {
        CanevasImporte c = new CanevasImporte(); // en-tête vide, aucun matériel

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        // référence, code poste, objet, saisisseur, chef mission, chef de poste, état du câblage, catégorie de câble
        assertThat(r.nbBloquants()).isEqualTo(8);
    }

    @Test
    void etat_du_cablage_et_categorie_de_cable_obligatoires() {
        CanevasImporte c = new CanevasImporte();
        EnteteMission e = c.getEntete();
        e.setReference("MIS-2026-001"); e.setCodePoste("TPR-01"); e.setObjet("Inventaire");
        e.setAgentSaisisseur("AG1"); e.setChefMission("AG1"); e.setChefPoste("CP1");
        // état du câblage et catégorie de câble laissés vides

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        assertThat(contient(r, Severite.BLOQUANT, "État du câblage réseau")).isTrue();
        assertThat(contient(r, Severite.BLOQUANT, "Catégorie de câble")).isTrue();
    }

    @Test
    void chef_de_poste_obligatoire() {
        CanevasImporte c = new CanevasImporte();
        EnteteMission e = c.getEntete();
        e.setReference("MIS-2026-001"); e.setCodePoste("TPR-01"); e.setObjet("Inventaire");
        e.setAgentSaisisseur("AG1"); e.setChefMission("AG1");
        e.setEtatCablage("Bon"); e.setCategorieCable("Cat6");
        // chef de poste laissé vide : le chargement doit être bloqué

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        assertThat(contient(r, Severite.BLOQUANT, "Chef de poste")).isTrue();
    }

    // ---------- statut obligatoire & état du réseau ----------

    @Test
    void statut_du_materiel_obligatoire() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        LigneOrdinateur o = ordinateurValide("ORD-1", MAC_VALIDE);
        o.setStatut("");
        c.getOrdinateurs().add(o);

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        assertThat(contient(r, Severite.BLOQUANT, "manquant : Statut")).isTrue();
    }

    @Test
    void statut_du_materiel_invalide_est_bloquant() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        LigneOrdinateur o = ordinateurValide("ORD-1", MAC_VALIDE);
        o.setStatut("Marche");
        c.getOrdinateurs().add(o);

        RapportImport r = controle.controler(c);

        assertThat(contient(r, Severite.BLOQUANT, "Statut invalide")).isTrue();
    }

    @Test
    void etat_du_reseau_hors_liste_est_bloquant() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getEntete().setEtatCablage("Excellent");

        RapportImport r = controle.controler(c);

        assertThat(contient(r, Severite.BLOQUANT, "État du réseau invalide")).isTrue();
    }

    @Test
    void etat_du_reseau_conforme_est_accepte() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getEntete().setEtatCablage("Bon");
        c.getOrdinateurs().add(ordinateurValide("ORD-1", MAC_VALIDE));

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isTrue();
        assertThat(contient(r, Severite.BLOQUANT, "État du réseau")).isFalse();
    }

    // ---------- onglet « Autres matériels » (types paramétrables) ----------

    private LigneAutreMateriel autre(String type, String nom, String mac) {
        LigneAutreMateriel a = new LigneAutreMateriel();
        a.setNumLigne(2);
        a.setTypeLibelle(type);
        a.setNom(nom);
        a.setMac(mac);
        return a;
    }

    @Test
    void autre_type_et_nom_sont_obligatoires() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getAutres().add(autre("", "", null));

        RapportImport r = controle.controler(c);

        assertThat(contient(r, Severite.BLOQUANT, "Type")).isTrue();
        assertThat(contient(r, Severite.BLOQUANT, "Nom")).isTrue();
    }

    @Test
    void autre_type_inconnu_est_bloquant() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getAutres().add(autre("Onduleur", "Onduleur salle serveur", null));
        when(categorieMaterielRepo.findByLibelleIgnoreCase("Onduleur")).thenReturn(Optional.empty());

        RapportImport r = controle.controler(c);

        assertThat(r.estIntegrable()).isFalse();
        assertThat(contient(r, Severite.BLOQUANT, "Type de matériel inconnu")).isTrue();
    }

    @Test
    void autre_doublon_mac_sans_numero_est_un_avertissement() {
        CanevasImporte c = new CanevasImporte();
        enteteValide(c);
        c.getAutres().add(autre("Onduleur", "Onduleur 1", MAC_VALIDE));
        when(categorieMaterielRepo.findByLibelleIgnoreCase("Onduleur")).thenReturn(Optional.of(new CategorieMateriel()));
        when(materielRepo.findByMac(MAC_VALIDE)).thenReturn(Optional.of(new Materiel()));

        RapportImport r = controle.controler(c);

        assertThat(r.nbAvertissements()).isEqualTo(1);
        assertThat(contient(r, Severite.AVERTISSEMENT, "déjà connu")).isTrue();
    }
}
