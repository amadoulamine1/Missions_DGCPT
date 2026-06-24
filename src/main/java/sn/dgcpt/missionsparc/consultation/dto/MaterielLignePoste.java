package sn.dgcpt.missionsparc.consultation.dto;

public class MaterielLignePoste {
    private final String numeroInventaire, type, nom, modele, statut, affecteA, caracteristiques;
    public MaterielLignePoste(String numeroInventaire, String type, String nom, String modele,
                              String statut, String affecteA, String caracteristiques) {
        this.numeroInventaire = numeroInventaire; this.type = type; this.nom = nom; this.modele = modele;
        this.statut = statut; this.affecteA = affecteA; this.caracteristiques = caracteristiques;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    public String getType() { return type; }
    public String getNom() { return nom; }
    public String getModele() { return modele; }
    public String getStatut() { return statut; }
    public String getAffecteA() { return affecteA; }
    public String getCaracteristiques() { return caracteristiques; }
}
