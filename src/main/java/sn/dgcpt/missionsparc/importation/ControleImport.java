package sn.dgcpt.missionsparc.importation;

import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.importation.dto.*;

import java.util.regex.Pattern;

/**
 * Contrôles à l'import (cf. Specification-import-canevas.md, §5).
 * Implémente ici les contrôles autonomes (formats, champs obligatoires).
 * Les contrôles nécessitant la base (agent connu, rapprochement) sont faits à l'intégration.
 */
@Component
public class ControleImport {

    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");

    public RapportImport controler(CanevasImporte c) {
        RapportImport r = new RapportImport();
        controlerEntete(c.getEntete(), r);

        for (LigneOrdinateur o : c.getOrdinateurs()) {
            r.incrementerLignesLues();
            obligatoire(o.getNomMachine(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "Nom machine", r);
            obligatoire(o.getAgentAttributaire(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "Agent attributaire", r);
            obligatoire(o.getAgentInstallateur(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "Agent installateur", r);
            macObligatoire(o.getMacEthernet(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "MAC ethernet", r);
            macSiPresent(o.getMacWifi(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "MAC wifi", r);
        }
        for (LigneImprimante i : c.getImprimantes()) {
            r.incrementerLignesLues();
            obligatoire(i.getNom(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), "Nom", r);
            macObligatoire(i.getMac(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), "MAC", r);
            macSiPresent(i.getMacWifi(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), "MAC wifi", r);
        }
        for (LigneEquipementReseau eq : c.getEquipementsReseau()) {
            r.incrementerLignesLues();
            obligatoire(eq.getType(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), "Type", r);
            obligatoire(eq.getNom(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), "Nom", r);
            macObligatoire(eq.getMac(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), "MAC", r);
        }
        for (LigneScanner sc : c.getScanners()) {
            r.incrementerLignesLues();
            obligatoire(sc.getNumeroSerie(), CanevasReader.SHEET_SCANNERS, sc.getNumLigne(), "Numéro de série", r);
        }
        return r;
    }

    private void controlerEntete(EnteteMission e, RapportImport r) {
        obligatoireEntete(e.getReference(), "N° de mission", r);
        obligatoireEntete(e.getCodePoste(), "Code poste", r);
        obligatoireEntete(e.getObjet(), "Objet de la mission", r);
        obligatoireEntete(e.getAgentSaisisseur(), "Agent saisisseur", r);
        obligatoireEntete(e.getChefMission(), "Chef de mission", r);
        obligatoireEntete(e.getChefPoste(), "Chef de poste", r);
    }

    private void obligatoireEntete(String v, String champ, RapportImport r) {
        if (estVide(v)) {
            r.ajouter(new AnomalieImport(Severite.BLOQUANT, CanevasReader.SHEET_MISSION, null,
                    "Champ obligatoire manquant : " + champ));
        }
    }

    private void obligatoire(String v, String onglet, int ligne, String champ, RapportImport r) {
        if (estVide(v)) {
            r.ajouter(new AnomalieImport(Severite.BLOQUANT, onglet, ligne,
                    "Champ obligatoire manquant : " + champ));
        }
    }

    private void macObligatoire(String v, String onglet, int ligne, String champ, RapportImport r) {
        if (estVide(v)) {
            r.ajouter(new AnomalieImport(Severite.BLOQUANT, onglet, ligne,
                    "Champ obligatoire manquant : " + champ));
        } else if (!MAC.matcher(v).matches()) {
            r.ajouter(new AnomalieImport(Severite.BLOQUANT, onglet, ligne,
                    "Format d'adresse MAC invalide (" + champ + ") : " + v));
        }
    }

    private void macSiPresent(String v, String onglet, int ligne, String champ, RapportImport r) {
        if (!estVide(v) && !MAC.matcher(v).matches()) {
            r.ajouter(new AnomalieImport(Severite.BLOQUANT, onglet, ligne,
                    "Format d'adresse MAC invalide (" + champ + ") : " + v));
        }
    }

    private boolean estVide(String v) {
        return v == null || v.isBlank();
    }
}
