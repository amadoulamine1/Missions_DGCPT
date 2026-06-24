package sn.dgcpt.missionsparc.mission;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.AgentRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

/** Produit un canevas pré-rempli pour une mission : en-tête, membres, et référentiels (vrais agents). */
@Component
public class CanevasWriter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AgentRepository agentRepo;

    public CanevasWriter(AgentRepository agentRepo) {
        this.agentRepo = agentRepo;
    }

    public byte[] prestamper(Mission m) throws IOException {
        try (InputStream is = new ClassPathResource("canevas/canevas-vierge.xlsx").getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            // 1) En-tête
            Sheet s = wb.getSheet("1-Mission et Réseau");
            remplir(s, "n° de mission", m.getReference());
            remplir(s, "code poste", m.getPoste() == null ? "" : m.getPoste().getCode());
            remplir(s, "nom du poste", m.getPoste() == null ? "" : m.getPoste().getNom());
            remplir(s, "objet", m.getObjet());
            remplir(s, "date de début", m.getDateDebut() == null ? "" : m.getDateDebut().format(FMT));
            remplir(s, "date de fin", m.getDateFin() == null ? "" : m.getDateFin().format(FMT));
            remplir(s, "chef de mission", libelle(m.getChefMission()));
            remplir(s, "chef de poste", libelle(m.getChefPosteFige()));

            // 2) Membres de la mission
            Sheet mem = wb.getSheet("2-Membres mission");
            if (mem != null) {
                int r = 1;
                for (Agent a : m.getMembres()) {
                    Row row = ligne(mem, r++);
                    set(row, 0, a.getMatricule());
                    set(row, 1, a.getNom());
                    set(row, 2, a.getPrenom());
                    set(row, 3, a.getFonction());
                    set(row, 4, a.getTelephone());
                    set(row, 5, a.getEmail());
                }
            }

            // 3) Référentiels : vrais agents (remplace les exemples)
            Sheet ref = wb.getSheet("Referentiels");
            if (ref != null && m.getPoste() != null) {
                for (int rr = 3; rr < 80; rr++) { vider(ref, rr, 0); vider(ref, rr, 2); }
                int ra = 3;
                for (Agent a : agentRepo.findByPoste_Id(m.getPoste().getId())) {
                    set(ligne(ref, ra++), 0, libelle(a));
                }
                int ri = 3;
                for (Agent a : agentRepo.findByTypeAgent(TypeAgent.INFORMATICIEN)) {
                    set(ligne(ref, ri++), 2, libelle(a));
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private String libelle(Agent a) {
        return a == null ? "" : a.getMatricule() + " — " + a.getNom() + " " + a.getPrenom();
    }

    private Row ligne(Sheet s, int rowIdx) {
        Row r = s.getRow(rowIdx);
        return (r != null) ? r : s.createRow(rowIdx);
    }

    private void set(Row row, int col, String val) {
        Cell c = row.getCell(col);
        if (c == null) c = row.createCell(col);
        c.setCellValue(val == null ? "" : val);
    }

    private void vider(Sheet s, int rowIdx, int col) {
        Row r = s.getRow(rowIdx);
        if (r == null) return;
        Cell c = r.getCell(col);
        if (c != null) c.setBlank();
    }

    private void remplir(Sheet s, String prefixeLabel, String valeur) {
        if (s == null) return;
        for (Row row : s) {
            Cell a = row.getCell(0);
            if (a == null || a.getCellType() != CellType.STRING) continue;
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
