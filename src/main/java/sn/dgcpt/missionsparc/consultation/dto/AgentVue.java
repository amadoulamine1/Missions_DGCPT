package sn.dgcpt.missionsparc.consultation.dto;

public class AgentVue {
    private final String matricule, nomComplet, fonction, telephone;
    public AgentVue(String matricule, String nomComplet, String fonction, String telephone) {
        this.matricule = matricule; this.nomComplet = nomComplet; this.fonction = fonction; this.telephone = telephone;
    }
    public String getMatricule() { return matricule; }
    public String getNomComplet() { return nomComplet; }
    public String getFonction() { return fonction; }
    public String getTelephone() { return telephone; }
}
