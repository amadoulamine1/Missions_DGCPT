package sn.dgcpt.missionsparc.consultation.dto;

/** Répartition d'un type de matériel par statut (pour les tableaux/barres du rapport annuel). */
public class TypeStat {
    private final String libelle;
    private final long total;
    private final long enService;
    private final long enPanne;
    private final long aChanger;

    public TypeStat(String libelle, long total, long enService, long enPanne, long aChanger) {
        this.libelle = libelle;
        this.total = total;
        this.enService = enService;
        this.enPanne = enPanne;
        this.aChanger = aChanger;
    }

    public String getLibelle() { return libelle; }
    public long getTotal() { return total; }
    public long getEnService() { return enService; }
    public long getEnPanne() { return enPanne; }
    public long getAChanger() { return aChanger; }
}
