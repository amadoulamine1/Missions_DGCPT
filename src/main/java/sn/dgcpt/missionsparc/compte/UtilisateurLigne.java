package sn.dgcpt.missionsparc.compte;

public class UtilisateurLigne {
    private final Integer id;
    private final String username, nomComplet, role;
    private final boolean actif;
    public UtilisateurLigne(Integer id, String username, String nomComplet, String role, boolean actif) {
        this.id = id; this.username = username; this.nomComplet = nomComplet; this.role = role; this.actif = actif;
    }
    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getNomComplet() { return nomComplet; }
    public String getRole() { return role; }
    public boolean isActif() { return actif; }
}
