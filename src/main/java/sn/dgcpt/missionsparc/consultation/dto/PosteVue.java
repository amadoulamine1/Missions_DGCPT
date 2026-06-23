package sn.dgcpt.missionsparc.consultation.dto;

public class PosteVue {
    private final Integer id;
    private final String code, nom, region;
    private final long nbMateriel;
    public PosteVue(Integer id, String code, String nom, String region, long nbMateriel) {
        this.id = id; this.code = code; this.nom = nom; this.region = region; this.nbMateriel = nbMateriel;
    }
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getNom() { return nom; }
    public String getRegion() { return region; }
    public long getNbMateriel() { return nbMateriel; }
}
