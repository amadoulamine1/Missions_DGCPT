package sn.dgcpt.missionsparc.importation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** Résultat du contrôle d'un canevas : anomalies + compteurs. */
@Getter
public class RapportImport {

    private final List<AnomalieImport> anomalies = new ArrayList<>();
    private int lignesLues;

    public void ajouter(AnomalieImport a) {
        anomalies.add(a);
    }

    public void incrementerLignesLues() {
        lignesLues++;
    }

    public long nbBloquants() {
        return anomalies.stream().filter(a -> a.getSeverite() == Severite.BLOQUANT).count();
    }

    public long nbAvertissements() {
        return anomalies.stream().filter(a -> a.getSeverite() == Severite.AVERTISSEMENT).count();
    }

    /** Vrai si aucune anomalie bloquante : le canevas peut être consolidé. */
    public boolean estIntegrable() {
        return nbBloquants() == 0;
    }
}
