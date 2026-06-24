package sn.dgcpt.missionsparc.importation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.importation.dto.*;

import java.io.IOException;
import java.io.InputStream;

/** Lit un canevas Excel (.xlsx) et le transforme en {@link CanevasImporte}. */
@Component
public class CanevasReader {

    public static final String SHEET_MISSION = "1-Mission et Réseau";
    public static final String SHEET_MEMBRES = "2-Membres mission";
    public static final String SHEET_ORDINATEURS = "3-Ordinateurs";
    public static final String SHEET_IMPRIMANTES = "4-Imprimantes";
    public static final String SHEET_RESEAU = "5-Switchs et AP";
    public static final String SHEET_SCANNERS = "6-Scanners chèque";
    public static final String SHEET_AGENTS = "Agents TPR";

    private final DataFormatter fmt = new DataFormatter();

    public CanevasImporte lire(InputStream is) throws IOException {
        try (Workbook wb = new XSSFWorkbook(is)) {
            CanevasImporte c = new CanevasImporte();
            lireEntete(wb.getSheet(SHEET_MISSION), c.getEntete());
            lireMembres(wb.getSheet(SHEET_MEMBRES), c);
            lireOrdinateurs(wb.getSheet(SHEET_ORDINATEURS), c);
            lireImprimantes(wb.getSheet(SHEET_IMPRIMANTES), c);
            lireReseau(wb.getSheet(SHEET_RESEAU), c);
            lireScanners(wb.getSheet(SHEET_SCANNERS), c);
            lireAgentsTpr(wb.getSheet(SHEET_AGENTS), c);
            return c;
        }
    }

    private String cell(Row row, int idx) {
        if (row == null) return "";
        Cell cell = row.getCell(idx);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }

    /** Extrait le matricule d'une valeur de liste "AG001 — DIOP Awa". */
    static String matricule(String v) {
        if (v == null || v.isBlank()) return "";
        int i = v.indexOf('—'); // tiret cadratin
        if (i < 0) i = v.indexOf('-');
        return (i > 0 ? v.substring(0, i) : v).trim();
    }

    private boolean oui(String v) {
        return v != null && v.trim().equalsIgnoreCase("Oui");
    }

    private boolean ligneVide(Row row, int nbCols) {
        for (int i = 0; i < nbCols; i++) {
            if (!cell(row, i).isEmpty()) return false;
        }
        return true;
    }

    private void lireEntete(Sheet s, EnteteMission e) {
        if (s == null) return;
        for (Row row : s) {
            String label = cell(row, 0).replace("*", "").trim().toLowerCase();
            String val = cell(row, 1);
            if (label.startsWith("n° de mission") || label.startsWith("no de mission")) e.setReference(val);
            else if (label.startsWith("code poste")) e.setCodePoste(val);
            else if (label.startsWith("nom du poste")) e.setNomPoste(val);
            else if (label.startsWith("objet")) e.setObjet(val);
            else if (label.startsWith("date de début")) e.setDateDebut(val);
            else if (label.startsWith("date de fin")) e.setDateFin(val);
            else if (label.startsWith("chef de mission")) e.setChefMission(matricule(val));
            else if (label.startsWith("chef de poste")) e.setChefPoste(matricule(val));
            else if (label.startsWith("agent saisisseur")) e.setAgentSaisisseur(matricule(val));
            else if (label.startsWith("zone")) e.setZone(val);
            else if (label.startsWith("état du câblage")) e.setEtatCablage(val);
            else if (label.startsWith("catégorie de câble")) e.setCategorieCable(val);
        }
    }

    private void lireMembres(Sheet s, CanevasImporte c) {
        if (s == null) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (ligneVide(row, 6)) continue;
            LigneMembre m = new LigneMembre();
            m.setNumLigne(r + 1);
            m.setMatricule(cell(row, 0));
            m.setNom(cell(row, 1));
            m.setPrenom(cell(row, 2));
            m.setFonction(cell(row, 3));
            m.setTelephone(cell(row, 4));
            m.setEmail(cell(row, 5));
            c.getMembres().add(m);
        }
    }

    private void lireOrdinateurs(Sheet s, CanevasImporte c) {
        if (s == null) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (ligneVide(row, 12)) continue;
            LigneOrdinateur o = new LigneOrdinateur();
            o.setNumLigne(r + 1);
            o.setNumeroInventaire(cell(row, 0));
            o.setNomMachine(cell(row, 1));
            o.setModele(cell(row, 2));
            o.setMacEthernet(cell(row, 3));
            o.setMacWifi(cell(row, 4));
            o.setAgentAttributaire(matricule(cell(row, 5)));
            o.setAgentInstallateur(matricule(cell(row, 6)));
            o.setAster(oui(cell(row, 7)));
            o.setAntivirus(oui(cell(row, 8)));
            o.setSicCDD(oui(cell(row, 9)));
            o.setCic(oui(cell(row, 10)));
            o.setSysbudget(oui(cell(row, 11)));
            c.getOrdinateurs().add(o);
        }
    }

    private void lireImprimantes(Sheet s, CanevasImporte c) {
        if (s == null) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (ligneVide(row, 6)) continue;
            LigneImprimante i = new LigneImprimante();
            i.setNumLigne(r + 1);
            i.setNumeroInventaire(cell(row, 0));
            i.setNom(cell(row, 1));
            i.setModele(cell(row, 2));
            i.setMac(cell(row, 3));
            i.setMacWifi(cell(row, 4));
            i.setIp(cell(row, 5));
            c.getImprimantes().add(i);
        }
    }

    private void lireReseau(Sheet s, CanevasImporte c) {
        if (s == null) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (ligneVide(row, 6)) continue;
            LigneEquipementReseau eq = new LigneEquipementReseau();
            eq.setNumLigne(r + 1);
            eq.setNumeroInventaire(cell(row, 0));
            eq.setType(cell(row, 1));
            eq.setNom(cell(row, 2));
            eq.setModele(cell(row, 3));
            eq.setMac(cell(row, 4));
            eq.setIp(cell(row, 5));
            c.getEquipementsReseau().add(eq);
        }
    }

    private void lireScanners(Sheet s, CanevasImporte c) {
        if (s == null) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (ligneVide(row, 4)) continue;
            LigneScanner sc = new LigneScanner();
            sc.setNumLigne(r + 1);
            sc.setNumeroInventaire(cell(row, 0));
            sc.setNumeroSerie(cell(row, 1));
            sc.setMarque(cell(row, 2));
            sc.setModele(cell(row, 3));
            c.getScanners().add(sc);
        }
    }

    private void lireAgentsTpr(Sheet s, CanevasImporte c) {
        if (s == null) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (ligneVide(row, 6)) continue;
            LigneAgentPoste a = new LigneAgentPoste();
            a.setNumLigne(r + 1);
            a.setMatricule(cell(row, 0));
            a.setNom(cell(row, 1));
            a.setPrenom(cell(row, 2));
            a.setFonction(cell(row, 3));
            a.setTelephone(cell(row, 4));
            a.setEmail(cell(row, 5));
            c.getAgentsTpr().add(a);
        }
    }
}
