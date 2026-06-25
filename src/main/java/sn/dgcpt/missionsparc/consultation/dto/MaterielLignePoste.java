package sn.dgcpt.missionsparc.consultation.dto;

public class MaterielLignePoste {
    private final String numeroInventaire, type, famille, nom, modele, statut, affecteA, caracteristiques, observation;
    public MaterielLignePoste(String numeroInventaire, String type, String famille, String nom, String modele,
                              String statut, String affecteA, String caracteristiques, String observation) {
        this.numeroInventaire = numeroInventaire; this.type = type; this.famille = famille; this.nom = nom; this.modele = modele;
        this.statut = statut; this.affecteA = affecteA; this.caracteristiques = caracteristiques; this.observation = observation;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    public String getType() { return type; }
    public String getFamille() { return famille; }
    public String getNom() { return nom; }
    public String getModele() { return modele; }
    public String getStatut() { return statut; }
    public String getAffecteA() { return affecteA; }
    public String getCaracteristiques() { return caracteristiques; }
    public String getObservation() { return observation; }
}
