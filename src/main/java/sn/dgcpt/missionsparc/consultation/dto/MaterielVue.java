package sn.dgcpt.missionsparc.consultation.dto;

public class MaterielVue {
    private final String numeroInventaire, type, nom, modele, posteNom;
    public MaterielVue(String numeroInventaire, String type, String nom, String modele, String posteNom) {
        this.numeroInventaire = numeroInventaire; this.type = type; this.nom = nom; this.modele = modele; this.posteNom = posteNom;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    public String getType() { return type; }
    public String getNom() { return nom; }
    public String getModele() { return modele; }
    public String getPosteNom() { return posteNom; }
}
