package sn.dgcpt.missionsparc.donnees;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import sn.dgcpt.missionsparc.audit.AuditService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Export / import de la base de données (réservé à l'administrateur). S'appuie sur les outils PostgreSQL
 * {@code pg_dump} / {@code pg_restore} (format « custom » compressé), comme les scripts de sauvegarde.
 * Le dossier des binaires est configurable par la propriété {@code app.pg-bin} (ou la variable
 * d'environnement {@code PG_BIN}) ; vide ⇒ recherche dans le PATH.
 */
@Service
public class DonneesService {

    private static final Pattern URL = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?;]+)");

    private final Environment env;
    private final AuditService audit;

    public DonneesService(Environment env, AuditService audit) {
        this.env = env;
        this.audit = audit;
    }

    /** Exporte toute la base (schéma + données) au format custom restaurable. */
    public byte[] exporter() throws IOException, InterruptedException {
        Conn c = conn();
        List<String> cmd = new ArrayList<>(List.of(pg("pg_dump"),
                "-h", c.host, "-p", c.port, "-U", c.user, "-F", "c", c.db));
        File err = File.createTempFile("pg_dump-err", ".log");
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectError(err);
            pb.environment().put("PGPASSWORD", c.password);
            Process p = demarrer(pb, "pg_dump");
            byte[] dump = p.getInputStream().readAllBytes();      // stdout = dump binaire
            int code = p.waitFor();
            if (code != 0) {
                throw new IllegalStateException("Export impossible (pg_dump, code " + code + ") : " + lire(err));
            }
            audit.tracer(AuditService.EXPORT_DONNEES, c.db, dump.length + " octets");
            return dump;
        } finally {
            err.delete();
        }
    }

    /**
     * Restaure la base à partir d'un fichier de sauvegarde (format custom). <b>Remplace les données.</b>
     * Atomique ({@code --single-transaction}) : en cas d'échec, la base reste inchangée.
     */
    public void importer(File fichier) throws IOException, InterruptedException {
        verifierFormatCustom(fichier);
        Conn c = conn();
        File err = File.createTempFile("pg_restore-err", ".log");
        try {
            List<String> cmd = new ArrayList<>(List.of(pg("pg_restore"),
                    "-h", c.host, "-p", c.port, "-U", c.user, "-d", c.db,
                    "--clean", "--if-exists", "--no-owner", "--single-transaction",
                    fichier.getAbsolutePath()));
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectError(err);
            pb.environment().put("PGPASSWORD", c.password);
            Process p = demarrer(pb, "pg_restore");
            p.getInputStream().readAllBytes(); // vider stdout
            int code = p.waitFor();
            if (code != 0) {
                throw new IllegalStateException("Import impossible (pg_restore, code " + code + ") : " + lire(err));
            }
            audit.tracer(AuditService.IMPORT_DONNEES, c.db, fichier.length() + " octets restaurés");
        } finally {
            err.delete();
        }
    }

    /** Vérifie l'en-tête « PGDMP » d'une sauvegarde PostgreSQL au format custom. */
    private void verifierFormatCustom(File fichier) throws IOException {
        byte[] entete = new byte[5];
        try (var in = Files.newInputStream(fichier.toPath())) {
            int lus = in.readNBytes(entete, 0, 5);
            if (lus < 5 || !"PGDMP".equals(new String(entete, StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException(
                        "Fichier invalide : attendu une sauvegarde PostgreSQL au format custom (.dump).");
            }
        }
    }

    /** Nom de fichier proposé pour l'export (base + horodatage). */
    public String nomFichierExport() {
        String db = conn().db;
        return db + "-" + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".dump";
    }

    // ---- helpers ----

    private record Conn(String host, String port, String db, String user, String password) { }

    private Conn conn() {
        String url = env.getProperty("spring.datasource.url", "");
        Matcher m = URL.matcher(url);
        if (!m.find()) {
            throw new IllegalStateException("URL de base non reconnue : " + url);
        }
        String host = m.group(1);
        String port = (m.group(2) != null) ? m.group(2) : "5432";
        String db = m.group(3);
        String user = env.getProperty("spring.datasource.username", "postgres");
        String password = env.getProperty("spring.datasource.password", "");
        return new Conn(host, port, db, user, password);
    }

    /**
     * Chemin de l'exécutable PostgreSQL. Priorité : (1) dossier configuré {@code app.pg-bin} / {@code PG_BIN} ;
     * (2) sous Windows, détection automatique de l'installation standard (le dossier {@code bin} de PostgreSQL
     * y est rarement dans le {@code PATH}) ; (3) sinon, recherche dans le {@code PATH} (cas typique Linux).
     */
    private String pg(String nom) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String exe = windows ? nom + ".exe" : nom;

        String bin = env.getProperty("app.pg-bin", "");
        if (!bin.isBlank()) {
            return Paths.get(bin, exe).toString();          // (1) dossier explicitement configuré
        }
        if (windows) {
            String detecte = detecterBinWindows(exe);       // (2) installation standard la plus récente
            if (detecte != null) {
                return detecte;
            }
        }
        return exe;                                          // (3) PATH
    }

    /** Cherche {@code <ProgramFiles>\PostgreSQL\<version>\bin\<exe>} et retourne la version la plus récente. */
    private String detecterBinWindows(String exe) {
        String[] racines = {
                System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"),
                "C:\\Program Files", "C:\\Program Files (x86)"
        };
        String meilleur = null;
        int meilleureVersion = -1;
        for (String racine : racines) {
            if (racine == null || racine.isBlank()) continue;
            File[] versions = new File(racine, "PostgreSQL").listFiles(File::isDirectory);
            if (versions == null) continue;
            for (File v : versions) {
                File candidat = new File(v, "bin" + File.separator + exe);
                if (!candidat.isFile()) continue;
                int num = numeroMajeur(v.getName());
                if (num > meilleureVersion) {
                    meilleureVersion = num;
                    meilleur = candidat.getAbsolutePath();
                }
            }
        }
        return meilleur;
    }

    /** Numéro de version majeure d'un dossier d'installation PostgreSQL (« 18 » → 18) ; -1 si non numérique. */
    private int numeroMajeur(String nom) {
        StringBuilder sb = new StringBuilder();
        for (char ch : nom.toCharArray()) {
            if (Character.isDigit(ch)) sb.append(ch); else break;
        }
        return sb.isEmpty() ? -1 : Integer.parseInt(sb.toString());
    }

    /** Démarre le processus en convertissant l'absence de binaire en message d'erreur actionnable. */
    private Process demarrer(ProcessBuilder pb, String outil) throws IOException {
        try {
            return pb.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Outil PostgreSQL « " + outil + " » introuvable. Installez les outils clients PostgreSQL, "
                    + "ou indiquez leur dossier via la propriété app.pg-bin (variable d'environnement PG_BIN), "
                    + "par ex. C:\\Program Files\\PostgreSQL\\18\\bin.", e);
        }
    }

    private String lire(File f) {
        try {
            return Files.readString(f.toPath()).trim();
        } catch (IOException e) {
            return "(détail indisponible)";
        }
    }
}
