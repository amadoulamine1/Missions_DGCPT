package sn.dgcpt.missionsparc.consultation.dto;

public class MaterielVue {
    private final String numeroInventaire, type, nom, modele, posteNom, statut, observation;
    public MaterielVue(String numeroInventaire, String type, String nom, String modele, String posteNom, String statut, String observation) {
        this.numeroInventaire = numeroInventaire; this.type = type; this.nom = nom; this.modele = modele; this.posteNom = posteNom; this.statut = statut; this.observation = observation;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    public String getType() { return type; }
    public String getNom() { return nom; }
    public String getModele() { return modele; }
    public String getPosteNom() { return posteNom; }
    public String getStatut() { return statut; }
    public String getObservation() { return observation; }
}
