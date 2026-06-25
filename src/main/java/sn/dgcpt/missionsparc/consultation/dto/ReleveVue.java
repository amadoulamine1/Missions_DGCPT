package sn.dgcpt.missionsparc.consultation.dto;

import java.time.LocalDate;

public class ReleveVue {
    private final String numeroInventaire, typeMateriel, nomMateriel, agentSaisisseur, statut, zone;
    private final LocalDate dateReleve;
    public ReleveVue(String numeroInventaire, String typeMateriel, String nomMateriel, String agentSaisisseur,
                     String statut, String zone, LocalDate dateReleve) {
        this.numeroInventaire = numeroInventaire; this.typeMateriel = typeMateriel; this.nomMateriel = nomMateriel;
        this.agentSaisisseur = agentSaisisseur; this.statut = statut; this.zone = zone; this.dateReleve = dateReleve;
    }
    public String getNumeroInventaire() { return numeroInventaire; }
    public String getTypeMateriel() { return typeMateriel; }
    public String getNomMateriel() { return nomMateriel; }
    /** Agent saisisseur au format « matricule — prénom nom ». */
    public String getAgentSaisisseur() { return agentSaisisseur; }
    public String getStatut() { return statut; }
    public String getZone() { return zone; }
    public LocalDate getDateReleve() { return dateReleve; }
}
