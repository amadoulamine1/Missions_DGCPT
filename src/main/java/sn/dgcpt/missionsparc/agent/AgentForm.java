package sn.dgcpt.missionsparc.agent;

public class AgentForm {
    private String matricule, nom, prenom, fonction, telephone, email;
    private String typeAgent = "POSTE";   // POSTE | INFORMATICIEN
    private Integer posteId;

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
    public String getTypeAgent() { return typeAgent; }
    public void setTypeAgent(String typeAgent) { this.typeAgent = typeAgent; }
    public Integer getPosteId() { return posteId; }
    public void setPosteId(Integer posteId) { this.posteId = posteId; }
}
