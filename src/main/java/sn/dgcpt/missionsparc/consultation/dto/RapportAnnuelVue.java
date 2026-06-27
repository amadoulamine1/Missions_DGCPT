package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

/** Données complètes du rapport annuel (année N, tendance ≤ 5 ans, prévision N+1). */
public class RapportAnnuelVue {

    private int annee;
    private int ans;                          // largeur de la fenêtre de tendance (2..5)
    private List<Integer> anneesDisponibles;  // pour le sélecteur d'année

    /** Indicateurs de pilotage (missions, relevés, incidents, nouveau matériel, taille parc, dispo). */
    private List<SerieAnnuelle> series;

    // --- Missions de l'année ---
    private List<String[]> missionsAnnee;     // [réf, objet, poste, période, chef, nbRelevés, état]
    private long[] missionsParMois;           // 12 cases (jan..déc)
    private List<StatPoste> missionsParPoste;
    private long postesCouverts;

    // --- Parc au 31/12 ---
    private long parcTaille, parcSvc, parcPan, parcChg, parcDispo;
    private List<TypeStat> parcParType;
    private List<StatPoste> parcParPoste;
    private List<TypeStat> nouveauParType;    // matériel ajouté dans l'année, par type
    private long nouveauTotal;

    // --- Incidents & maintenance ---
    private List<TypeStat> incidentsParType;  // pannes/à changer relevés dans l'année
    private List<StatPoste> incidentsParPoste;
    private List<String[]> incidentsListe;    // [n°, type, nom, poste, statut, date]
    private long reaffectations;

    // --- Activité des agents ---
    private List<String[]> activiteAgents;    // [matricule, nom, nbMissions]

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public int getAns() { return ans; }
    public void setAns(int ans) { this.ans = ans; }
    public List<Integer> getAnneesDisponibles() { return anneesDisponibles; }
    public void setAnneesDisponibles(List<Integer> v) { this.anneesDisponibles = v; }
    public List<SerieAnnuelle> getSeries() { return series; }
    public void setSeries(List<SerieAnnuelle> v) { this.series = v; }

    public List<String[]> getMissionsAnnee() { return missionsAnnee; }
    public void setMissionsAnnee(List<String[]> v) { this.missionsAnnee = v; }
    public long[] getMissionsParMois() { return missionsParMois; }
    public void setMissionsParMois(long[] v) { this.missionsParMois = v; }
    public long getMissionsParMoisMax() {
        long mx = 0;
        if (missionsParMois != null) for (long x : missionsParMois) mx = Math.max(mx, x);
        return mx;
    }
    public List<StatPoste> getMissionsParPoste() { return missionsParPoste; }
    public void setMissionsParPoste(List<StatPoste> v) { this.missionsParPoste = v; }
    public long getPostesCouverts() { return postesCouverts; }
    public void setPostesCouverts(long v) { this.postesCouverts = v; }

    public long getParcTaille() { return parcTaille; }
    public void setParcTaille(long v) { this.parcTaille = v; }
    public long getParcSvc() { return parcSvc; }
    public void setParcSvc(long v) { this.parcSvc = v; }
    public long getParcPan() { return parcPan; }
    public void setParcPan(long v) { this.parcPan = v; }
    public long getParcChg() { return parcChg; }
    public void setParcChg(long v) { this.parcChg = v; }
    public long getParcDispo() { return parcDispo; }
    public void setParcDispo(long v) { this.parcDispo = v; }
    public List<TypeStat> getParcParType() { return parcParType; }
    public void setParcParType(List<TypeStat> v) { this.parcParType = v; }
    public List<StatPoste> getParcParPoste() { return parcParPoste; }
    public void setParcParPoste(List<StatPoste> v) { this.parcParPoste = v; }
    public List<TypeStat> getNouveauParType() { return nouveauParType; }
    public void setNouveauParType(List<TypeStat> v) { this.nouveauParType = v; }
    public long getNouveauTotal() { return nouveauTotal; }
    public void setNouveauTotal(long v) { this.nouveauTotal = v; }

    public List<TypeStat> getIncidentsParType() { return incidentsParType; }
    public void setIncidentsParType(List<TypeStat> v) { this.incidentsParType = v; }
    public List<StatPoste> getIncidentsParPoste() { return incidentsParPoste; }
    public void setIncidentsParPoste(List<StatPoste> v) { this.incidentsParPoste = v; }
    public List<String[]> getIncidentsListe() { return incidentsListe; }
    public void setIncidentsListe(List<String[]> v) { this.incidentsListe = v; }
    public long getReaffectations() { return reaffectations; }
    public void setReaffectations(long v) { this.reaffectations = v; }

    public List<String[]> getActiviteAgents() { return activiteAgents; }
    public void setActiviteAgents(List<String[]> v) { this.activiteAgents = v; }
}
