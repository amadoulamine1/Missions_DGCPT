package sn.dgcpt.missionsparc.audit;

/** Ligne du journal d'audit pour l'affichage (date pré-formatée). */
public class AuditVue {
    private final String dateHeure, utilisateur, action, cible, detail;
    public AuditVue(String dateHeure, String utilisateur, String action, String cible, String detail) {
        this.dateHeure = dateHeure; this.utilisateur = utilisateur; this.action = action;
        this.cible = cible; this.detail = detail;
    }
    public String getDateHeure() { return dateHeure; }
    public String getUtilisateur() { return utilisateur; }
    public String getAction() { return action; }
    public String getCible() { return cible; }
    public String getDetail() { return detail; }
}
