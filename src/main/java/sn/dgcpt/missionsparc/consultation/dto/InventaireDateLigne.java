package sn.dgcpt.missionsparc.consultation.dto;

public class InventaireDateLigne {
    private final String numeroInventaire, type, nom, modele, poste, affecteA, depuis, etatObserve;
    public InventaireDateLigne(String numeroInventaire, String type, String nom, String modele,
                               String poste, String affecteA, String depuis, String etatObserve) {
        this.numeroInventaire = numeroInventaire; this.type = type; this.nom = nom; this.modele = modele;
        this.poste = poste; this.affecteA = affecteA; this.depuis = depuis; this.etatObserve = etatObserve;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    public String getType() { return type; }
    public String getNom() { return nom; }
    public String getModele() { return modele; }
    public String getPoste() { return poste; }
    public String getAffecteA() { return affecteA; }
    public String getDepuis() { return depuis; }
    public String getEtatObserve() { return etatObserve; }
}
