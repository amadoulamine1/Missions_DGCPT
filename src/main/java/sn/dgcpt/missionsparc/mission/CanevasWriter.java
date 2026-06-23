package sn.dgcpt.missionsparc.mission;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.domain.Mission;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

/** Produit un canevas dont l'en-tête (onglet 1) est pré-rempli pour une mission. */
@Component
public class CanevasWriter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] prestamper(Mission m) throws IOException {
        try (InputStream is = new ClassPathResource("canevas/canevas-vierge.xlsx").getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {
            Sheet s = wb.getSheet("1-Mission et Réseau");
            remplir(s, "n° de mission", m.getReference());
            remplir(s, "code poste", m.getPoste() == null ? "" : m.getPoste().getCode());
            remplir(s, "nom du poste", m.getPoste() == null ? "" : m.getPoste().getNom());
            remplir(s, "objet", m.getObjet());
            remplir(s, "date de début", m.getDateDebut() == null ? "" : m.getDateDebut().format(FMT));
            remplir(s, "date de fin", m.getDateFin() == null ? "" : m.getDateFin().format(FMT));
            remplir(s, "chef de mission", m.getChefMission() == null ? "" : m.getChefMission().getMatricule());
            remplir(s, "chef de poste", m.getChefPosteFige() == null ? "" : m.getChefPosteFige().getMatricule());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void remplir(Sheet s, String prefixeLabel, String valeur) {
        if (s == null) return;
        for (Row row : s) {
            Cell a = row.getCell(0);
            if (a == null || a.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING) continue;
            String lab = a.getStringCellValue().replace("*", "").trim().toLowerCase();
            if (lab.startsWith(prefixeLabel)) {
                Cell b = row.getCell(1);
                if (b == null) b = row.createCell(1);
                b.setCellValue(valeur == null ? "" : valeur);
                return;
            }
        }
    }
}
