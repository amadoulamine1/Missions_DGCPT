package sn.dgcpt.missionsparc.mission;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.AffectationMaterielRepository;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.EquipementReseauRepository;
import sn.dgcpt.missionsparc.repository.ImprimanteRepository;
import sn.dgcpt.missionsparc.repository.MaterielRepository;
import sn.dgcpt.missionsparc.repository.OrdinateurRepository;
import sn.dgcpt.missionsparc.repository.ScannerChequeRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

/** Produit un canevas pré-rempli pour une mission : en-tête, membres, et référentiels (vrais agents). */
@Component
public class CanevasWriter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AgentRepository agentRepo;
    private final MaterielRepository materielRepo;
    private final OrdinateurRepository ordinateurRepo;
    private final ImprimanteRepository imprimanteRepo;
    private final EquipementReseauRepository reseauRepo;
    private final ScannerChequeRepository scannerRepo;
    private final AffectationMaterielRepository affectationRepo;

    public CanevasWriter(AgentRepository agentRepo, MaterielRepository materielRepo,
                         OrdinateurRepository ordinateurRepo, ImprimanteRepository imprimanteRepo,
                         EquipementReseauRepository reseauRepo, ScannerChequeRepository scannerRepo,
                         AffectationMaterielRepository affectationRepo) {
        this.agentRepo = agentRepo;
        this.materielRepo = materielRepo;
        this.ordinateurRepo = ordinateurRepo;
        this.imprimanteRepo = imprimanteRepo;
        this.reseauRepo = reseauRepo;
        this.scannerRepo = scannerRepo;
        this.affectationRepo = affectationRepo;
    }

    public byte[] prestamper(Mission m) throws IOException {
        try (InputStream is = new ClassPathResource("canevas/canevas-vierge.xlsx").getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            wb.setForceFormulaRecalculation(true);

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
            remplir(s, "observations", m.getObservations());

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

            // 3) Référentiels : membres de la mission (colonne A) -> listes "agent saisisseur" et "agent traitant"
            Sheet ref = wb.getSheet("Referentiels");
            if (ref != null) {
                for (int rr = 3; rr < 210; rr++) { vider(ref, rr, 0); }
                int rm = 3;
                for (Agent a : m.getMembres()) { set(ligne(ref, rm++), 0, libelle(a)); }
            }

            // 4) Feuille "Agents TPR" : agents du poste (attributaires), pré-chargés et complétables
            Sheet agtpr = wb.getSheet("Agents TPR");
            if (agtpr != null && m.getPoste() != null) {
                for (int rr = 1; rr < 210; rr++) { for (int cc = 0; cc < 6; cc++) { vider(agtpr, rr, cc); } }
                int r = 1;
                for (Agent a : agentRepo.findByPoste_Id(m.getPoste().getId())) {
                    Row row = ligne(agtpr, r++);
                    set(row, 0, a.getMatricule());
                    set(row, 1, a.getNom());
                    set(row, 2, a.getPrenom());
                    set(row, 3, a.getFonction());
                    set(row, 4, a.getTelephone());
                    set(row, 5, a.getEmail());
                    set(row, 7, libelle(a)); // colonne H : libellé "matricule — prénom nom" (valeur, pour la liste attributaire)
                }
            }

            // 5) Inventaire déjà enregistré pour le poste (l'agent vérifie / complète)
            fillInventaire(wb, m);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private String libelle(Agent a) {
        return a == null ? "" : a.getMatricule() + " — " + a.getPrenom() + " " + a.getNom();
    }

    /** Pré-remplit les feuilles matériel avec l'inventaire déjà enregistré pour le poste de la mission. */
    private void fillInventaire(Workbook wb, Mission m) {
        if (m.getPoste() == null) return;
        Sheet sOrd = wb.getSheet("3-Ordinateurs");
        Sheet sImp = wb.getSheet("4-Imprimantes");
        Sheet sRes = wb.getSheet("5-Switchs et AP");
        Sheet sScn = wb.getSheet("6-Scanners chèque");
        int rOrd = 1, rImp = 1, rRes = 1, rScn = 1;
        for (Materiel mat : materielRepo.findByPoste_Id(m.getPoste().getId())) {
            String num = mat.getNumeroInventaire();
            String statut = statutLabel(mat.getStatut());
            String obs = mat.getObservation();
            switch (mat.getType()) {
                case ORDINATEUR -> {
                    Ordinateur o = ordinateurRepo.findById(num).orElse(null);
                    Row row = ligne(sOrd, rOrd++);
                    set(row, 0, num);
                    set(row, 1, o != null ? o.getNomMachine() : mat.getNom());
                    set(row, 2, mat.getModele());
                    if (o != null) {
                        set(row, 3, o.getMacEthernet());
                        set(row, 4, o.getMacWifi());
                        set(row, 6, libelle(o.getAgentInstallateur()));
                        Set<String> logs = o.getLogiciels().stream().map(Logiciel::getNom).collect(Collectors.toSet());
                        set(row, 7, logs.contains("Aster") ? "Oui" : "Non");
                        set(row, 8, logs.contains("Antivirus") ? "Oui" : "Non");
                        set(row, 9, logs.contains("SicCDD") ? "Oui" : "Non");
                        set(row, 10, logs.contains("CIC") ? "Oui" : "Non");
                        set(row, 11, logs.contains("Sysbudget") ? "Oui" : "Non");
                        set(row, 12, o.getRam());
                        set(row, 13, o.getProcesseur());
                        set(row, 14, o.getDisqueDur());
                    }
                    set(row, 5, attributaire(mat));
                    set(row, 15, statut);
                    set(row, 16, obs);
                }
                case IMPRIMANTE -> {
                    Imprimante i = imprimanteRepo.findById(num).orElse(null);
                    Row row = ligne(sImp, rImp++);
                    set(row, 0, num); set(row, 1, mat.getNom()); set(row, 2, mat.getModele());
                    if (i != null) { set(row, 3, i.getMac()); set(row, 4, i.getMacWifi()); set(row, 5, i.getIp()); }
                    set(row, 6, statut); set(row, 7, obs);
                }
                case SWITCH, ACCESS_POINT -> {
                    EquipementReseau e = reseauRepo.findById(num).orElse(null);
                    Row row = ligne(sRes, rRes++);
                    set(row, 0, num);
                    set(row, 1, mat.getType() == TypeMateriel.ACCESS_POINT ? "Access point" : "Switch");
                    set(row, 2, mat.getNom()); set(row, 3, mat.getModele());
                    if (e != null) { set(row, 4, e.getMac()); set(row, 5, e.getIp()); }
                    set(row, 6, statut); set(row, 7, obs);
                }
                case SCANNER_CHEQUE -> {
                    ScannerCheque sc = scannerRepo.findById(num).orElse(null);
                    Row row = ligne(sScn, rScn++);
                    set(row, 0, num);
                    if (sc != null) { set(row, 1, sc.getNumeroSerie()); set(row, 2, sc.getMarque()); }
                    set(row, 3, mat.getModele());
                    set(row, 4, statut); set(row, 5, obs);
                }
                default -> { }
            }
        }
    }

    private String attributaire(Materiel mat) {
        return affectationRepo.findByMaterielAndDateFinIsNull(mat)
                .map(AffectationMateriel::getAgent)
                .map(this::libelle).orElse("");
    }

    private String statutLabel(StatutMateriel s) {
        if (s == null) return "";
        return switch (s) {
            case EN_SERVICE -> "En service";
            case EN_PANNE -> "En panne";
            case A_CHANGER -> "À changer";
        };
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
