package sn.dgcpt.missionsparc.importation.dto;


public class LigneMembre {
    private int numLigne;
    private String matricule;
    private String nom;
    private String prenom;
    private String fonction;
    private String telephone;
    private String email;

    public int getNumLigne() { return numLigne; }
    public void setNumLigne(int numLigne) { this.numLigne = numLigne; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getFonction() { return fonction; }
    public void setFonction(String fonction) { this.fonction = fonction; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
