package sn.dgcpt.missionsparc.agent;

public class AgentLigne {
    private final String matricule, nomComplet, fonction, type, posteNom;
    public AgentLigne(String matricule, String nomComplet, String fonction, String type, String posteNom) {
        this.matricule = matricule; this.nomComplet = nomComplet; this.fonction = fonction; this.type = type; this.posteNom = posteNom;
    }
    public String getMatricule() { return matricule; }
    public String getNomComplet() { return nomComplet; }
    public String getFonction() { return fonction; }
    public String getType() { return type; }
    public String getPosteNom() { return posteNom; }
}
