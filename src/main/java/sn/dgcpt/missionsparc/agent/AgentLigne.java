package sn.dgcpt.missionsparc.agent;

public class AgentLigne {
    private final String matricule, nomComplet, fonction, type, posteNom, telephone, email;
    private final Integer posteId;
    public AgentLigne(String matricule, String nomComplet, String fonction, String type, String posteNom, Integer posteId,
                      String telephone, String email) {
        this.matricule = matricule; this.nomComplet = nomComplet; this.fonction = fonction; this.type = type;
        this.posteNom = posteNom; this.posteId = posteId; this.telephone = telephone; this.email = email;
    }
    public String getMatricule() { return matricule; }
    public String getNomComplet() { return nomComplet; }
    public String getFonction() { return fonction; }
    public String getType() { return type; }
    public String getPosteNom() { return posteNom; }
    public Integer getPosteId() { return posteId; }
    public String getTelephone() { return telephone; }
    public String getEmail() { return email; }
}
