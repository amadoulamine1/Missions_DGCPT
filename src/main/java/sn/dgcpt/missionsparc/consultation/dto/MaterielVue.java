package sn.dgcpt.missionsparc.consultation.dto;

public class MaterielVue {
    private final String numeroInventaire, type, famille, nom, modele, posteNom, statut, observation;
    private final String ram, processeur, disqueDur;
    public MaterielVue(String numeroInventaire, String type, String famille, String nom, String modele, String posteNom,
                       String statut, String observation, String ram, String processeur, String disqueDur) {
        this.numeroInventaire = numeroInventaire; this.type = type; this.famille = famille; this.nom = nom; this.modele = modele;
        this.posteNom = posteNom; this.statut = statut; this.observation = observation;
        this.ram = ram; this.processeur = processeur; this.disqueDur = disqueDur;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    /** Libellé du type paramétrable (affichage). */
    public String getType() { return type; }
    /** Famille technique (ORDINATEUR, IMPRIMANTE…) pour les glyphes/couleurs. */
    public String getFamille() { return famille; }
    public String getNom() { return nom; }
    public String getModele() { return modele; }
    public String getPosteNom() { return posteNom; }
    public String getStatut() { return statut; }
    public String getObservation() { return observation; }
    public String getRam() { return ram; }
    public String getProcesseur() { return processeur; }
    public String getDisqueDur() { return disqueDur; }
}
