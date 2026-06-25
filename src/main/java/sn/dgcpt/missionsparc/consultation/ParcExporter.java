package sn.dgcpt.missionsparc.consultation;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.consultation.dto.MaterielVue;
import sn.dgcpt.missionsparc.consultation.dto.ReleveVue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ParcExporter {

    public byte[] exporter(List<MaterielVue> materiels) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Parc");
            CellStyle entete = wb.createCellStyle();
            Font gras = wb.createFont(); gras.setBold(true); entete.setFont(gras);

            String[] cols = {"N° inventaire", "Type", "Nom", "Modèle", "RAM", "Processeur", "Disque", "Poste", "Statut", "Observation"};
            Row h = s.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(entete);
            }
            int r = 1;
            for (MaterielVue m : materiels) {
                Row row = s.createRow(r++);
                row.createCell(0).setCellValue(nz(m.getNumeroInventaire()));
                row.createCell(1).setCellValue(nz(m.getType()));
                row.createCell(2).setCellValue(nz(m.getNom()));
                row.createCell(3).setCellValue(nz(m.getModele()));
                row.createCell(4).setCellValue(nz(m.getRam()));
                row.createCell(5).setCellValue(nz(m.getProcesseur()));
                row.createCell(6).setCellValue(nz(m.getDisqueDur()));
                row.createCell(7).setCellValue(nz(m.getPosteNom()));
                row.createCell(8).setCellValue(nz(m.getStatut()));
                row.createCell(9).setCellValue(nz(m.getObservation()));
            }
            for (int i = 0; i < cols.length; i++) s.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exporterReleves(List<ReleveVue> releves) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Relevés");
            CellStyle entete = wb.createCellStyle();
            Font gras = wb.createFont(); gras.setBold(true); entete.setFont(gras);
            String[] cols = {"N° inventaire", "Type", "Nom", "Saisi par", "Statut", "Zone", "Date"};
            Row h = s.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(entete);
            }
            int r = 1;
            for (ReleveVue v : releves) {
                Row row = s.createRow(r++);
                row.createCell(0).setCellValue(nz(v.getNumeroInventaire()));
                row.createCell(1).setCellValue(nz(v.getTypeMateriel()));
                row.createCell(2).setCellValue(nz(v.getNomMateriel()));
                row.createCell(3).setCellValue(nz(v.getAgentSaisisseur()));
                row.createCell(4).setCellValue(nz(v.getStatut()));
                row.createCell(5).setCellValue(nz(v.getZone()));
                row.createCell(6).setCellValue(v.getDateReleve() == null ? "" : v.getDateReleve().toString());
            }
            for (int i = 0; i < cols.length; i++) s.autoSizeColumn(i);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private String nz(String x) { return x == null ? "" : x; }
}
