package sn.dgcpt.missionsparc.consultation.dto;

public class StatPoste {
    private final String nom;
    private final long total, enPanne;
    public StatPoste(String nom, long total, long enPanne) {
        this.nom = nom; this.total = total; this.enPanne = enPanne;
    }
    public String getNom() { return nom; }
    public long getTotal() { return total; }
    public long getEnPanne() { return enPanne; }
}
