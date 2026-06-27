package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

/**
 * Série annuelle d'un indicateur de pilotage (historique ≤ 5 ans) + éventuel point de prévision N+1.
 * Expose les valeurs prêtes pour l'affichage : valeur de l'année (N), écart vs N‑1, prévision, et le
 * maximum pour mettre les barres à l'échelle.
 */
public class SerieAnnuelle {

    /** Un point de la série : une année, sa valeur, et un drapeau « prévu » (extrapolation N+1). */
    public static class Point {
        private final int annee;
        private final long valeur;
        private final boolean prevu;
        public Point(int annee, long valeur, boolean prevu) { this.annee = annee; this.valeur = valeur; this.prevu = prevu; }
        public int getAnnee() { return annee; }
        public long getValeur() { return valeur; }
        public boolean isPrevu() { return prevu; }
    }

    private final String libelle;
    private final String unite;          // "" pour un effectif, "%" pour un taux
    private final List<Point> points;    // historique, suivi éventuellement du point prévu (prevu=true)
    private final int sens;              // +1 = hausse favorable, -1 = baisse favorable, 0 = neutre

    public SerieAnnuelle(String libelle, String unite, int sens, List<Point> points) {
        this.libelle = libelle;
        this.unite = unite;
        this.sens = sens;
        this.points = points;
    }

    public String getLibelle() { return libelle; }
    public String getUnite() { return unite; }
    public int getSens() { return sens; }
    public List<Point> getPoints() { return points; }

    /** Sens de l'écart pour la flèche : "up" / "down" / "flat", ou {@code null} si pas d'année N‑1. */
    public String getSensEcart() {
        Long d = getDelta();
        if (d == null) return null;
        return d > 0 ? "up" : (d < 0 ? "down" : "flat");
    }

    /** {@code true} = écart favorable, {@code false} = défavorable, {@code null} = neutre (pour la couleur). */
    public Boolean getEcartFavorable() {
        Long d = getDelta();
        if (d == null || sens == 0 || d == 0) return null;
        return (sens > 0) == (d > 0);
    }

    private List<Point> historique() { return points.stream().filter(p -> !p.isPrevu()).toList(); }

    public long getValeurN() {
        List<Point> h = historique();
        return h.isEmpty() ? 0 : h.get(h.size() - 1).getValeur();
    }

    public Long getValeurN1() {
        List<Point> h = historique();
        return h.size() < 2 ? null : h.get(h.size() - 2).getValeur();
    }

    /** Écart N − (N‑1), ou {@code null} si l'année précédente n'est pas disponible. */
    public Long getDelta() {
        Long n1 = getValeurN1();
        return n1 == null ? null : getValeurN() - n1;
    }

    /** Point de prévision N+1, ou {@code null} si non projetable. */
    public Point getPrevision() {
        return points.stream().filter(Point::isPrevu).findFirst().orElse(null);
    }

    /** Valeur maximale (historique + prévision) pour mettre les barres à l'échelle. */
    public long getMax() {
        return points.stream().mapToLong(Point::getValeur).max().orElse(0);
    }
}
