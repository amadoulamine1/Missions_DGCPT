package sn.dgcpt.missionsparc.importation;


/** Une anomalie détectée lors du contrôle d'un canevas. */
public class AnomalieImport {
    private final Severite severite;
    private final String onglet;
    private final Integer ligne;   // null pour l'en-tête
    private final String message;

    public AnomalieImport(Severite severite, String onglet, Integer ligne, String message) {
        this.severite = severite;
        this.onglet = onglet;
        this.ligne = ligne;
        this.message = message;
    }

    @Override
    public String toString() {
        String pos = (ligne != null) ? onglet + " ligne " + ligne : onglet;
        return "[" + severite + "] " + pos + " : " + message;
    }

    public Severite getSeverite() { return severite; }
    public String getOnglet() { return onglet; }
    public Integer getLigne() { return ligne; }
    public String getMessage() { return message; }
}
