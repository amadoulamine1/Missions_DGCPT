package sn.dgcpt.missionsparc.consultation;

import java.util.List;

/**
 * Prévision par <b>régression linéaire</b> (moindres carrés) sur une série annuelle.
 * Utilisée par le rapport annuel pour projeter l'année N+1 à partir de l'historique disponible
 * (≤ 5 ans). C'est une <b>extrapolation</b> indicative, pas une certitude.
 */
public final class Prevision {

    private Prevision() {}

    /**
     * Projette la valeur de l'année suivante à partir d'une série chronologique ordonnée
     * (une valeur par année consécutive). Renvoie {@code null} si moins de 2 points (extrapolation
     * impossible). Les indicateurs étant des effectifs/pourcentages ≥ 0, le résultat est borné à 0.
     */
    public static Double projeterAnneeSuivante(List<? extends Number> serie) {
        if (serie == null || serie.size() < 2) return null;
        int n = serie.size();
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            double x = i, y = serie.get(i).doubleValue();
            sx += x; sy += y; sxx += x * x; sxy += x * y;
        }
        double denom = n * sxx - sx * sx;
        if (denom == 0) return Math.max(0d, serie.get(n - 1).doubleValue()); // série dégénérée
        double pente = (n * sxy - sx * sy) / denom;
        double ordonnee = (sy - pente * sx) / n;
        double prevision = ordonnee + pente * n; // x = n ⇒ année suivante
        return Math.max(0d, prevision);
    }

    /** Variante arrondie à l'entier le plus proche (effectifs). {@code null} si non projetable. */
    public static Long projeterEntier(List<? extends Number> serie) {
        Double p = projeterAnneeSuivante(serie);
        return p == null ? null : Math.round(p);
    }
}
