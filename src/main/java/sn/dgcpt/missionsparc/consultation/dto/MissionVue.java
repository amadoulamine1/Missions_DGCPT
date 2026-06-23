package sn.dgcpt.missionsparc.consultation.dto;

public class MissionVue {
    private final Integer id;
    private final String reference, objet, posteNom, periode, statut;
    public MissionVue(Integer id, String reference, String objet, String posteNom, String periode, String statut) {
        this.id = id; this.reference = reference; this.objet = objet; this.posteNom = posteNom; this.periode = periode; this.statut = statut;
    }
    public Integer getId() { return id; }
    public String getReference() { return reference; }
    public String getObjet() { return objet; }
    public String getPosteNom() { return posteNom; }
    public String getPeriode() { return periode; }
    public String getStatut() { return statut; }
}
