package sn.dgcpt.missionsparc.compte;

public class UtilisateurForm {
    private Integer id;
    private String username;
    private String nomComplet;
    private String role = "AGENT";
    private String motDePasse;
    private boolean actif = true;
    private String agentMatricule;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public String getAgentMatricule() { return agentMatricule; }
    public void setAgentMatricule(String agentMatricule) { this.agentMatricule = agentMatricule; }
}
