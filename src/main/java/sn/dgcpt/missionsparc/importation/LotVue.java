package sn.dgcpt.missionsparc.importation;

public class LotVue {
    private final Integer id;
    private final String saisisseur, fichier, date;
    private final int nbLignes;
    public LotVue(Integer id, String saisisseur, String fichier, String date, int nbLignes) {
        this.id = id; this.saisisseur = saisisseur; this.fichier = fichier; this.date = date; this.nbLignes = nbLignes;
    }
    public Integer getId() { return id; }
    public String getSaisisseur() { return saisisseur; }
    public String getFichier() { return fichier; }
    public String getDate() { return date; }
    public int getNbLignes() { return nbLignes; }
}
