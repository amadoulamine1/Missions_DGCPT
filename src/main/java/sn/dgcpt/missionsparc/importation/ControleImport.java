package sn.dgcpt.missionsparc.importation;

import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.importation.dto.*;
import sn.dgcpt.missionsparc.repository.EquipementReseauRepository;
import sn.dgcpt.missionsparc.repository.ImprimanteRepository;
import sn.dgcpt.missionsparc.repository.OrdinateurRepository;
import sn.dgcpt.missionsparc.repository.ScannerChequeRepository;

import java.util.regex.Pattern;

/**
 * Contrôles à l'import (cf. Specification-import-canevas.md, §5).
 * Contrôles autonomes (formats, champs obligatoires) + alerte anti-doublon :
 * une ligne sans n° d'inventaire mais dont la MAC / le n° de série est déjà connu en base
 * est signalée (AVERTISSEMENT) au chef de mission avant validation.
 */
@Component
public class ControleImport {

    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");

    private final OrdinateurRepository ordinateurRepo;
    private final ImprimanteRepository imprimanteRepo;
    private final EquipementReseauRepository reseauRepo;
    private final ScannerChequeRepository scannerRepo;

    public ControleImport(OrdinateurRepository ordinateurRepo, ImprimanteRepository imprimanteRepo,
                          EquipementReseauRepository reseauRepo, ScannerChequeRepository scannerRepo) {
        this.ordinateurRepo = ordinateurRepo;
        this.imprimanteRepo = imprimanteRepo;
        this.reseauRepo = reseauRepo;
        this.scannerRepo = scannerRepo;
    }

    public RapportImport controler(CanevasImporte c) {
        RapportImport r = new RapportImport();
        controlerEntete(c.getEntete(), r);

        for (LigneOrdinateur o : c.getOrdinateurs()) {
            r.incrementerLignesLues();
            obligatoire(o.getNomMachine(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "Nom machine", r);
            obligatoire(o.getAgentAttributaire(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "Agent attributaire", r);
            obligatoire(o.getAgentInstallateur(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "Agent traitant", r);
            macObligatoire(o.getMacEthernet(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "MAC ethernet", r);
            macSiPresent(o.getMacWifi(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), "MAC wifi", r);
            if (estNouveau(o.getNumeroInventaire()) && macValide(o.getMacEthernet())
                    && ordinateurRepo.findByMacEthernet(o.getMacEthernet()).isPresent()) {
                alerteDoublon("MAC " + o.getMacEthernet(), CanevasReader.SHEET_ORDINATEURS, o.getNumLigne(), r);
            }
        }
        for (LigneImprimante i : c.getImprimantes()) {
            r.incrementerLignesLues();
            obligatoire(i.getNom(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), "Nom", r);
            macObligatoire(i.getMac(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), "MAC", r);
            macSiPresent(i.getMacWifi(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), "MAC wifi", r);
            if (estNouveau(i.getNumeroInventaire()) && macValide(i.getMac())
                    && imprimanteRepo.findByMac(i.getMac()).isPresent()) {
                alerteDoublon("MAC " + i.getMac(), CanevasReader.SHEET_IMPRIMANTES, i.getNumLigne(), r);
            }
        }
        for (LigneEquipementReseau eq : c.getEquipementsReseau()) {
            r.incrementerLignesLues();
            obligatoire(eq.getType(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), "Type", r);
            obligatoire(eq.getNom(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), "Nom", r);
            macObligatoire(eq.getMac(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), "MAC", r);
            if (estNouveau(eq.getNumeroInventaire()) && macValide(eq.getMac())
                    && reseauRepo.findByMac(eq.getMac()).isPresent()) {
                alerteDoublon("MAC " + eq.getMac(), CanevasReader.SHEET_RESEAU, eq.getNumLigne(), r);
            }
        }
        for (LigneScanner sc : c.getScanners()) {
            r.incrementerLignesLues();
            obligatoire(sc.getNumeroSerie(), CanevasReader.SHEET_SCANNERS, sc.getNumLigne(), "Numéro de série", r);
            if (estNouveau(sc.getNumeroInventaire()) && !estVide(sc.getNumeroSerie())
                    && scannerRepo.findByNumeroSerie(sc.getNumeroSerie()).isPresent()) {
                alerteDoublon("n° de série " + sc.getNumeroSerie(), CanevasReader.SHEET_SCANNERS, sc.getNumLigne(), r);
            }
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

    private void alerteDoublon(String cle, String onglet, int ligne, RapportImport r) {
        r.ajouter(new AnomalieImport(Severite.AVERTISSEMENT, onglet, ligne,
                "Sans n° d'inventaire mais « " + cle + " » déjà connu en base : ce matériel existe peut-être déjà — vérifiez avant de valider."));
    }

    private boolean estNouveau(String numero) {
        return estVide(numero);
    }

    private boolean macValide(String v) {
        return !estVide(v) && MAC.matcher(v).matches();
    }

    private boolean estVide(String v) {
        return v == null || v.isBlank();
    }
}
