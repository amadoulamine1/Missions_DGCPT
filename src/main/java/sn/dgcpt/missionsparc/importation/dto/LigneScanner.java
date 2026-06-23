package sn.dgcpt.missionsparc.importation.dto;


public class LigneScanner {
    private int numLigne;
    private String numeroInventaire;
    private String numeroSerie;
    private String marque;
    private String modele;

    public int getNumLigne() { return numLigne; }
    public void setNumLigne(int numLigne) { this.numLigne = numLigne; }
    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
}
