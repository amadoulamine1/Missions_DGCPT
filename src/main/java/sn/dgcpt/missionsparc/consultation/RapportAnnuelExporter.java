package sn.dgcpt.missionsparc.consultation;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.consultation.dto.RapportAnnuelVue;
import sn.dgcpt.missionsparc.consultation.dto.SerieAnnuelle;
import sn.dgcpt.missionsparc.consultation.dto.StatPoste;
import sn.dgcpt.missionsparc.consultation.dto.TypeStat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** Génère le classeur Excel du rapport annuel (synthèse, tendance, missions, parc, incidents, agents). */
@Component
public class RapportAnnuelExporter {

    public byte[] exporter(RapportAnnuelVue r) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle entete = wb.createCellStyle();
            Font gras = wb.createFont(); gras.setBold(true); entete.setFont(gras);

            int n = r.getAnnee();

            // 1) Synthèse annuelle
            Sheet syn = wb.createSheet("Synthèse " + n);
            ligne(syn, 0, entete, "Indicateur", String.valueOf(n), String.valueOf(n - 1), "Écart", "Prévision " + (n + 1));
            int rs = 1;
            for (SerieAnnuelle s : r.getSeries()) {
                Row row = syn.createRow(rs++);
                row.createCell(0).setCellValue(s.getLibelle());
                row.createCell(1).setCellValue(s.getValeurN());
                cellOpt(row, 2, s.getValeurN1());
                cellOpt(row, 3, s.getDelta());
                cellOpt(row, 4, s.getPrevision() == null ? null : s.getPrevision().getValeur());
            }
            autoSize(syn, 5);

            // 2) Tendance (≤ 5 ans)
            Sheet tnd = wb.createSheet("Tendance");
            List<SerieAnnuelle.Point> ref = r.getSeries().get(0).getPoints();
            Object[] hdr = new Object[1 + ref.size()];
            hdr[0] = "Indicateur";
            for (int i = 0; i < ref.size(); i++) {
                SerieAnnuelle.Point p = ref.get(i);
                hdr[i + 1] = p.isPrevu() ? ("Prév. " + p.getAnnee()) : String.valueOf(p.getAnnee());
            }
            ligneObj(tnd, 0, entete, hdr);
            int rt = 1;
            for (SerieAnnuelle s : r.getSeries()) {
                Row row = tnd.createRow(rt++);
                row.createCell(0).setCellValue(s.getLibelle());
                List<SerieAnnuelle.Point> pts = s.getPoints();
                for (int i = 0; i < pts.size(); i++) row.createCell(i + 1).setCellValue(pts.get(i).getValeur());
            }
            autoSize(tnd, 1 + ref.size());

            // 3) Missions de l'année
            Sheet mis = wb.createSheet("Missions " + n);
            ligne(mis, 0, entete, "Référence", "Objet", "Poste", "Période", "Chef de mission", "Relevés", "État");
            int rm = 1;
            for (String[] l : r.getMissionsAnnee()) ligneTexte(mis, rm++, l);
            autoSize(mis, 7);

            // 4) Parc au 31/12
            Sheet pc = wb.createSheet("Parc 31-12");
            ligne(pc, 0, entete, "Parc au 31/12/" + n, "Valeur");
            int rp = 1;
            rp = kv(pc, rp, "Taille du parc", r.getParcTaille());
            rp = kv(pc, rp, "En service", r.getParcSvc());
            rp = kv(pc, rp, "En panne", r.getParcPan());
            rp = kv(pc, rp, "À changer", r.getParcChg());
            rp = kv(pc, rp, "Disponibilité (%)", r.getParcDispo());
            rp = kv(pc, rp, "Matériel nouvellement inventorié", r.getNouveauTotal());
            rp++;
            ligne(pc, rp++, entete, "Par type", "Total", "En service", "En panne", "À changer");
            for (TypeStat t : r.getParcParType()) {
                Row row = pc.createRow(rp++);
                row.createCell(0).setCellValue(t.getLibelle());
                row.createCell(1).setCellValue(t.getTotal());
                row.createCell(2).setCellValue(t.getEnService());
                row.createCell(3).setCellValue(t.getEnPanne());
                row.createCell(4).setCellValue(t.getAChanger());
            }
            rp++;
            ligne(pc, rp++, entete, "Par poste", "Total", "En panne");
            for (StatPoste sp : r.getParcParPoste()) {
                Row row = pc.createRow(rp++);
                row.createCell(0).setCellValue(sp.getNom());
                row.createCell(1).setCellValue(sp.getTotal());
                row.createCell(2).setCellValue(sp.getEnPanne());
            }
            autoSize(pc, 5);

            // 5) Incidents
            Sheet inc = wb.createSheet("Incidents " + n);
            ligne(inc, 0, entete, "Type", "Total", "En panne", "À changer");
            int ri = 1;
            for (TypeStat t : r.getIncidentsParType()) {
                Row row = inc.createRow(ri++);
                row.createCell(0).setCellValue(t.getLibelle());
                row.createCell(1).setCellValue(t.getTotal());
                row.createCell(2).setCellValue(t.getEnPanne());
                row.createCell(3).setCellValue(t.getAChanger());
            }
            ri++;
            ligne(inc, ri++, entete, "N° inventaire", "Type", "Nom", "Poste", "Statut", "Date");
            for (String[] l : r.getIncidentsListe()) ligneTexte(inc, ri++, l);
            autoSize(inc, 6);

            // 6) Activité des agents
            Sheet ag = wb.createSheet("Agents " + n);
            ligne(ag, 0, entete, "Matricule", "Nom", "Missions");
            int ra = 1;
            for (String[] l : r.getActiviteAgents()) ligneTexte(ag, ra++, l);
            autoSize(ag, 3);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void ligne(Sheet s, int r, CellStyle st, String... vals) {
        Row row = s.createRow(r);
        for (int i = 0; i < vals.length; i++) { Cell c = row.createCell(i); c.setCellValue(vals[i]); c.setCellStyle(st); }
    }

    private void ligneObj(Sheet s, int r, CellStyle st, Object... vals) {
        Row row = s.createRow(r);
        for (int i = 0; i < vals.length; i++) { Cell c = row.createCell(i); c.setCellValue(String.valueOf(vals[i])); c.setCellStyle(st); }
    }

    private void ligneTexte(Sheet s, int r, String[] vals) {
        Row row = s.createRow(r);
        for (int i = 0; i < vals.length; i++) row.createCell(i).setCellValue(vals[i] == null ? "" : vals[i]);
    }

    private int kv(Sheet s, int r, String k, long v) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(k);
        row.createCell(1).setCellValue(v);
        return r + 1;
    }

    private void cellOpt(Row row, int col, Long v) {
        Cell c = row.createCell(col);
        if (v == null) c.setCellValue("—"); else c.setCellValue(v);
    }

    private void autoSize(Sheet s, int cols) {
        for (int i = 0; i < cols; i++) s.autoSizeColumn(i);
    }
}
