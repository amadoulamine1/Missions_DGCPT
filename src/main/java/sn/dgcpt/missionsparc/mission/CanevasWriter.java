package sn.dgcpt.missionsparc.mission;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.domain.*;
import sn.dgcpt.missionsparc.repository.AffectationMaterielRepository;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.CategorieMaterielRepository;
import sn.dgcpt.missionsparc.repository.EquipementReseauRepository;
import sn.dgcpt.missionsparc.repository.ImprimanteRepository;
import sn.dgcpt.missionsparc.repository.MaterielRepository;
import sn.dgcpt.missionsparc.repository.OrdinateurRepository;
import sn.dgcpt.missionsparc.repository.ScannerChequeRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final CategorieMaterielRepository categorieMaterielRepo;

    public CanevasWriter(AgentRepository agentRepo, MaterielRepository materielRepo,
                         OrdinateurRepository ordinateurRepo, ImprimanteRepository imprimanteRepo,
                         EquipementReseauRepository reseauRepo, ScannerChequeRepository scannerRepo,
                         AffectationMaterielRepository affectationRepo, CategorieMaterielRepository categorieMaterielRepo) {
        this.agentRepo = agentRepo;
        this.materielRepo = materielRepo;
        this.ordinateurRepo = ordinateurRepo;
        this.imprimanteRepo = imprimanteRepo;
        this.reseauRepo = reseauRepo;
        this.scannerRepo = scannerRepo;
        this.affectationRepo = affectationRepo;
        this.categorieMaterielRepo = categorieMaterielRepo;
    }

    public byte[] prestamper(Mission m) throws IOException {
        return prestamper(m, null);
    }

    /** Variante par agent : pré-renseigne l'agent saisisseur (cellule B11 de « 1-Mission et Réseau »). */
    public byte[] prestamper(Mission m, Agent saisisseur) throws IOException {
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
            if (saisisseur != null) remplir(s, "agent saisisseur", libelle(saisisseur));
            // État du réseau contraint à : Neuf / Bon / Pas bon
            listeDeroulanteCelluleLabel(s, "état du câblage", new String[]{"Neuf", "Bon", "Pas bon"});
            // Champs obligatoires de l'en-tête : marqués « * » et surlignés en rouge tant que vides
            marquerObligatoireEntete(s, "état du câblage");
            marquerObligatoireEntete(s, "catégorie de câble");

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

            // Statut obligatoire : surlignage rouge tant que vide sur une ligne saisie (onglets matériel)
            mefcStatut(wb.getSheet("3-Ordinateurs"), 16, 17);
            mefcStatut(wb.getSheet("4-Imprimantes"), 6, 7);
            mefcStatut(wb.getSheet("5-Switchs et AP"), 6, 7);
            mefcStatut(wb.getSheet("6-Scanners chèque"), 4, 5);

            // 6) Onglet générique des types paramétrables (famille AUTRE)
            fillAutres(wb, m);

            // 7) Ordre des feuilles : « Agents TPR » après « 1-Mission et Réseau »,
            //    « 7-Autres matériels » juste avant « Referentiels »
            int idxMission = wb.getSheetIndex("1-Mission et Réseau");
            if (idxMission >= 0 && wb.getSheet("Agents TPR") != null) {
                wb.setSheetOrder("Agents TPR", idxMission + 1);
            }
            int idxRef = wb.getSheetIndex("Referentiels");
            if (idxRef >= 0 && wb.getSheet("7-Autres matériels") != null) {
                wb.setSheetOrder("7-Autres matériels", idxRef);
            }

            // 8) Le N° d'inventaire est attribué par l'application : il ne doit pas être modifié
            //    dans le canevas. On verrouille la colonne A de toutes les feuilles de matériel.
            verrouillerNumeroInventaire(wb);

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
                        // Agent traitant (col. 6) NON pré-rempli : historisé par mission, à ressaisir à chaque mission.
                        Set<String> logs = o.getLogiciels().stream().map(Logiciel::getNom).collect(Collectors.toSet());
                        set(row, 7, logs.contains("Aster") ? "Oui" : "Non");
                        set(row, 8, logs.contains("Antivirus") ? "Oui" : "Non");
                        set(row, 9, logs.contains("SicCDD") ? "Oui" : "Non");
                        set(row, 10, logs.contains("CIC") ? "Oui" : "Non");
                        set(row, 11, logs.contains("Sysbudget") ? "Oui" : "Non");
                        set(row, 12, logs.contains("AD") ? "Oui" : "Non");
                        set(row, 13, o.getRam());
                        set(row, 14, o.getProcesseur());
                        set(row, 15, o.getDisqueDur());
                    }
                    set(row, 5, attributaire(mat));
                    set(row, 16, statut);
                    set(row, 17, obs);
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

    /**
     * Onglet « 7-Autres matériels » créé par programme (le canevas vierge ne le contient pas) :
     * en-têtes, listes déroulantes (type paramétrable famille AUTRE actif, statut) et pré-remplissage
     * des matériels génériques déjà connus du poste.
     */
    private void fillAutres(Workbook wb, Mission m) {
        Sheet s = wb.getSheet("7-Autres matériels");
        if (s == null) s = wb.createSheet("7-Autres matériels");

        String[] entetes = {"N° inventaire", "Type*", "Nom*", "Modèle", "MAC", "IP", "Statut*", "Observation"};
        int[] largeurs = {18, 16, 22, 18, 18, 14, 12, 30};
        XSSFCellStyle styleEntete = styleEntete((XSSFWorkbook) wb);
        Row tete = ligne(s, 0);
        tete.setHeightInPoints(20);
        for (int i = 0; i < entetes.length; i++) {
            set(tete, i, entetes[i]);
            tete.getCell(i).setCellStyle(styleEntete);
            s.setColumnWidth(i, largeurs[i] * 256);
        }
        s.createFreezePane(0, 1); // fige la ligne d'en-tête

        // Listes déroulantes : type (catégories famille AUTRE actives) en col. B, statut en col. G.
        List<String> types = categorieMaterielRepo
                .findByFamilleAndActifTrueOrderByLibelle(TypeMateriel.AUTRE).stream()
                .map(CategorieMateriel::getLibelle).collect(Collectors.toList());
        listeDeroulante(s, 1, types.toArray(new String[0]));
        listeDeroulante(s, 6, new String[]{"En service", "En panne", "À changer"});

        // Garde-fous (§5.2) : validation du format MAC et mise en forme conditionnelle des champs.
        validationMac(s, 4); // colonne E (MAC)
        miseEnFormeAutres(s);

        if (m.getPoste() == null) return;
        int r = 1;
        for (Materiel mat : materielRepo.findByPoste_Id(m.getPoste().getId())) {
            if (mat.getType() != TypeMateriel.AUTRE) continue;
            Row row = ligne(s, r++);
            set(row, 0, mat.getNumeroInventaire());
            set(row, 1, mat.getCategorie() == null ? "" : mat.getCategorie().getLibelle());
            set(row, 2, mat.getNom());
            set(row, 3, mat.getModele());
            set(row, 4, mat.getMac());
            set(row, 5, mat.getIp());
            set(row, 6, statutLabel(mat.getStatut()));
            set(row, 7, mat.getObservation());
        }
    }

    /** Indice de la ligne dont la colonne A commence par le libellé donné, ou -1. */
    private int ligneLabel(Sheet s, String prefixeLabel) {
        if (s == null) return -1;
        for (Row row : s) {
            Cell a = row.getCell(0);
            if (a == null || a.getCellType() != CellType.STRING) continue;
            String lab = a.getStringCellValue().replace("*", "").trim().toLowerCase();
            if (lab.startsWith(prefixeLabel)) return row.getRowNum();
        }
        return -1;
    }

    /** Pose une liste déroulante sur la cellule B (valeur) de la ligne portant le libellé donné. */
    private void listeDeroulanteCelluleLabel(Sheet s, String prefixeLabel, String[] valeurs) {
        int r = ligneLabel(s, prefixeLabel);
        if (r < 0 || valeurs.length == 0) return;
        DataValidationHelper helper = s.getDataValidationHelper();
        DataValidationConstraint c = helper.createExplicitListConstraint(valeurs);
        DataValidation v = helper.createValidation(c, new CellRangeAddressList(r, r, 1, 1));
        v.setSuppressDropDownArrow(true);
        v.setShowErrorBox(true);
        s.addValidationData(v);
    }

    /** Pose une validation « liste » sur une colonne (lignes 2 à 500). Excel limite la liste à 255 caractères. */
    private void listeDeroulante(Sheet s, int col, String[] valeurs) {
        if (valeurs.length == 0) return;
        int total = 0;
        for (String v : valeurs) total += v.length() + 1;
        if (total > 255) return; // trop de valeurs pour une liste explicite : saisie libre
        DataValidationHelper helper = s.getDataValidationHelper();
        DataValidationConstraint contrainte = helper.createExplicitListConstraint(valeurs);
        CellRangeAddressList zone = new CellRangeAddressList(1, 500, col, col);
        DataValidation validation = helper.createValidation(contrainte, zone);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        s.addValidationData(validation);
    }

    /** Formule Excel (OOXML, en anglais) vérifiant la structure d'une MAC AA:BB:CC:DD:EE:FF pour la cellule {@code ref}. */
    private String formuleMac(String ref) {
        return "AND(LEN(" + ref + ")=17,MID(" + ref + ",3,1)=\":\",MID(" + ref + ",6,1)=\":\","
                + "MID(" + ref + ",9,1)=\":\",MID(" + ref + ",12,1)=\":\",MID(" + ref + ",15,1)=\":\")";
    }

    /** Validation de saisie du format MAC (avertissement non bloquant ; cellule vide autorisée). */
    private void validationMac(Sheet s, int col) {
        DataValidationHelper helper = s.getDataValidationHelper();
        DataValidationConstraint c = helper.createCustomConstraint(formuleMac("E2"));
        CellRangeAddressList zone = new CellRangeAddressList(1, 500, col, col);
        DataValidation v = helper.createValidation(c, zone);
        v.setEmptyCellAllowed(true);
        v.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        v.setShowErrorBox(true);
        v.createErrorBox("Adresse MAC", "Format attendu : AA:BB:CC:DD:EE:FF");
        v.setShowPromptBox(true);
        v.createPromptBox("Adresse MAC", "Saisir au format AA:BB:CC:DD:EE:FF (facultatif).");
        s.addValidationData(v);
    }

    /**
     * Mise en forme conditionnelle de l'onglet « Autres matériels » : un champ obligatoire vide sur une
     * ligne saisie apparaît en rouge (Type col. B, Nom col. C) ; une MAC mal formée apparaît en orange (col. E).
     */
    private void miseEnFormeAutres(Sheet s) {
        SheetConditionalFormatting scf = s.getSheetConditionalFormatting();
        // ligne « saisie » = au moins une valeur entre A et H
        String ligneSaisie = "COUNTA($A2:$H2)>0";
        regleFond(scf, "B2:B501", "AND(LEN(TRIM(B2))=0," + ligneSaisie + ")", IndexedColors.ROSE);
        regleFond(scf, "C2:C501", "AND(LEN(TRIM(C2))=0," + ligneSaisie + ")", IndexedColors.ROSE);
        regleFond(scf, "G2:G501", "AND(LEN(TRIM(G2))=0," + ligneSaisie + ")", IndexedColors.ROSE); // statut obligatoire
        regleFond(scf, "E2:E501", "AND(LEN(TRIM(E2))>0,NOT(" + formuleMac("E2") + "))", IndexedColors.LIGHT_ORANGE);
    }

    private void regleFond(SheetConditionalFormatting scf, String plage, String formule, IndexedColors couleur) {
        ConditionalFormattingRule regle = scf.createConditionalFormattingRule(formule);
        PatternFormatting fond = regle.createPatternFormatting();
        fond.setFillBackgroundColor(couleur.index);
        fond.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        scf.addConditionalFormatting(new CellRangeAddress[]{CellRangeAddress.valueOf(plage)}, regle);
    }

    /** Style d'en-tête doré (fond or foncé, texte blanc gras) pour l'onglet créé par programme. */
    private XSSFCellStyle styleEntete(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x70, (byte) 0x56, (byte) 0x00}, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.LEFT);
        st.setBorderBottom(BorderStyle.THIN);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        st.setFont(font);
        return st;
    }

    /** Surligne en rouge la colonne « statut » (vide sur une ligne saisie) d'un onglet matériel. */
    private void mefcStatut(Sheet s, int colStatut, int dernCol) {
        if (s == null) return;
        String col = CellReference.convertNumToColString(colStatut);
        String last = CellReference.convertNumToColString(dernCol);
        String formule = "AND(LEN(TRIM(" + col + "2))=0,COUNTA($A2:$" + last + "2)>0)";
        regleFond(s.getSheetConditionalFormatting(), col + "2:" + col + "501", formule, IndexedColors.ROSE);
    }

    /** Marque un champ d'en-tête obligatoire : « * » sur le libellé (col. A) et fond rouge si la valeur (col. B) est vide. */
    private void marquerObligatoireEntete(Sheet s, String prefixeLabel) {
        int r = ligneLabel(s, prefixeLabel);
        if (r < 0) return;
        Cell a = s.getRow(r).getCell(0);
        if (a != null && a.getCellType() == CellType.STRING && !a.getStringCellValue().contains("*")) {
            a.setCellValue(a.getStringCellValue().trim() + " *");
        }
        String cell = "B" + (r + 1);
        regleFond(s.getSheetConditionalFormatting(), cell + ":" + cell, "LEN(TRIM(" + cell + "))=0", IndexedColors.ROSE);
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

    /**
     * Rend la colonne « N° inventaire » (A) non modifiable sur toutes les feuilles de matériel.
     * Les 4 feuilles câblées sont déjà protégées par le modèle (cellules de saisie déverrouillées) :
     * on y verrouille simplement la colonne A. L'onglet « 7-Autres matériels », généré par programme,
     * n'est pas protégé : on le protège en laissant la saisie possible (colonnes B…) mais en
     * verrouillant la colonne A.
     */
    private void verrouillerNumeroInventaire(Workbook wb) {
        for (String nom : List.of("3-Ordinateurs", "4-Imprimantes", "5-Switchs et AP", "6-Scanners chèque")) {
            Sheet s = wb.getSheet(nom);
            if (s != null) verrouillerColonneA(s);
        }
        Sheet autres = wb.getSheet("7-Autres matériels");
        if (autres != null) {
            deverrouillerSaisie(autres, 1, 7);   // Type, Nom, Modèle, MAC, IP, Statut, Observation
            verrouillerColonneA(autres);
            autres.protectSheet("");              // active la protection (cellules verrouillées = lecture seule)
        }
    }

    /** Verrouille les cellules de la colonne A (conserve l'apparence des données ; n'altère pas l'en-tête). */
    private void verrouillerColonneA(Sheet s) {
        Row r2 = s.getRow(1);
        Cell modele = (r2 != null) ? r2.getCell(0) : null;
        CellStyle verrou = s.getWorkbook().createCellStyle();
        if (modele != null && modele.getCellStyle() != null) verrou.cloneStyleFrom(modele.getCellStyle());
        verrou.setLocked(true);
        s.setDefaultColumnStyle(0, verrou);       // cellules vides / lignes ajoutées par l'agent
        for (Row row : s) {                       // cellules A existantes (sauf l'en-tête)
            if (row.getRowNum() == 0) continue;
            Cell c = row.getCell(0);
            if (c != null) c.setCellStyle(verrou);
        }
    }

    /** Déverrouille (saisie possible sous protection) les colonnes [colDeb..colFin], hors en-tête. */
    private void deverrouillerSaisie(Sheet s, int colDeb, int colFin) {
        CellStyle saisie = s.getWorkbook().createCellStyle();
        saisie.setLocked(false);
        for (int col = colDeb; col <= colFin; col++) s.setDefaultColumnStyle(col, saisie);
        for (Row row : s) {
            if (row.getRowNum() == 0) continue;
            for (int col = colDeb; col <= colFin; col++) {
                Cell c = row.getCell(col);
                if (c != null) c.setCellStyle(saisie);
            }
        }
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
