package sn.dgcpt.missionsparc.consultation;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.consultation.dto.StatPoste;
import sn.dgcpt.missionsparc.domain.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Génère un classeur Excel récapitulant les statistiques du tableau de bord. */
@Component
public class StatsExporter {

    public byte[] exporter(List<Materiel> parc, List<Mission> missions,
                           long nbPostes, long nbInfo, long nbAgentsPoste) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle entete = wb.createCellStyle();
            Font gras = wb.createFont(); gras.setBold(true); entete.setFont(gras);

            long total = parc.size();
            long svc = countStatut(parc, StatutMateriel.EN_SERVICE);
            long pan = countStatut(parc, StatutMateriel.EN_PANNE);
            long chg = countStatut(parc, StatutMateriel.A_CHANGER);
            long dispo = total > 0 ? svc * 100 / total : 0;

            LocalDate today = LocalDate.now();
            long plan = missions.stream().filter(m -> m.getDateDebut() != null && m.getDateDebut().isAfter(today)).count();
            long enCours = missions.stream().filter(m ->
                    !(m.getDateDebut() != null && m.getDateDebut().isAfter(today))
                    && (m.getDateFin() == null || !m.getDateFin().isBefore(today))).count();
            long term = missions.size() - plan - enCours;

            Sheet syn = wb.createSheet("Synthèse");
            ligne(syn, 0, entete, "Indicateur", "Valeur");
            int r = 1;
            r = kv(syn, r, "Postes (TPR)", nbPostes);
            r = kv(syn, r, "Matériel total", total);
            r = kv(syn, r, "En service", svc);
            r = kv(syn, r, "En panne", pan);
            r = kv(syn, r, "À changer", chg);
            r = kv(syn, r, "Taux de disponibilité (%)", dispo);
            r = kv(syn, r, "Informaticiens", nbInfo);
            r = kv(syn, r, "Agents de poste", nbAgentsPoste);
            r = kv(syn, r, "Missions", missions.size());
            r = kv(syn, r, "Missions planifiées", plan);
            r = kv(syn, r, "Missions en cours", enCours);
            r = kv(syn, r, "Missions terminées", term);
            syn.autoSizeColumn(0); syn.autoSizeColumn(1);

            Sheet pt = wb.createSheet("Par type");
            ligne(pt, 0, entete, "Type", "Total", "En service", "En panne", "À changer");
            int rt = 1;
            rt = typeRow(pt, rt, "Ordinateurs", parc, TypeMateriel.ORDINATEUR);
            rt = typeRow(pt, rt, "Imprimantes", parc, TypeMateriel.IMPRIMANTE);
            rt = typeRow(pt, rt, "Switchs / AP", parc, TypeMateriel.SWITCH, TypeMateriel.ACCESS_POINT);
            rt = typeRow(pt, rt, "Scanners chèque", parc, TypeMateriel.SCANNER_CHEQUE);
            for (int i = 0; i < 5; i++) pt.autoSizeColumn(i);

            Sheet pp = wb.createSheet("Par poste");
            ligne(pp, 0, entete, "Poste", "Total", "En panne");
            int rp = 1;
            for (StatPoste sp : parPoste(parc)) {
                Row row = pp.createRow(rp++);
                row.createCell(0).setCellValue(sp.getNom());
                row.createCell(1).setCellValue(sp.getTotal());
                row.createCell(2).setCellValue(sp.getEnPanne());
            }
            for (int i = 0; i < 3; i++) pp.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    public static List<StatPoste> parPoste(List<Materiel> parc) {
        Map<String, long[]> agg = new LinkedHashMap<>();
        for (Materiel m : parc) {
            String nom = m.getPoste() == null ? "(sans poste)" : m.getPoste().getNom();
            long[] a = agg.computeIfAbsent(nom, k -> new long[2]);
            a[0]++;
            if (m.getStatut() == StatutMateriel.EN_PANNE) a[1]++;
        }
        List<StatPoste> list = new ArrayList<>();
        for (Map.Entry<String, long[]> e : agg.entrySet())
            list.add(new StatPoste(e.getKey(), e.getValue()[0], e.getValue()[1]));
        list.sort(Comparator.comparingLong(StatPoste::getTotal).reversed());
        return list;
    }

    private int kv(Sheet s, int r, String k, long v) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(k);
        row.createCell(1).setCellValue(v);
        return r + 1;
    }

    private void ligne(Sheet s, int r, CellStyle st, String... vals) {
        Row row = s.createRow(r);
        for (int i = 0; i < vals.length; i++) { Cell c = row.createCell(i); c.setCellValue(vals[i]); c.setCellStyle(st); }
    }

    private int typeRow(Sheet s, int r, String label, List<Materiel> parc, TypeMateriel... types) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(countType(parc, types));
        row.createCell(2).setCellValue(countTypeStatut(parc, StatutMateriel.EN_SERVICE, types));
        row.createCell(3).setCellValue(countTypeStatut(parc, StatutMateriel.EN_PANNE, types));
        row.createCell(4).setCellValue(countTypeStatut(parc, StatutMateriel.A_CHANGER, types));
        return r + 1;
    }

    private long countStatut(List<Materiel> parc, StatutMateriel st) {
        return parc.stream().filter(m -> m.getStatut() == st).count();
    }
    private long countType(List<Materiel> parc, TypeMateriel... types) {
        return parc.stream().filter(m -> contient(types, m.getType())).count();
    }
    private long countTypeStatut(List<Materiel> parc, StatutMateriel st, TypeMateriel... types) {
        return parc.stream().filter(m -> m.getStatut() == st && contient(types, m.getType())).count();
    }
    private boolean contient(TypeMateriel[] types, TypeMateriel t) {
        for (TypeMateriel x : types) if (x == t) return true;
        return false;
    }
}
