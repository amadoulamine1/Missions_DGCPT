package sn.dgcpt.missionsparc.mission;

import java.util.ArrayList;
import java.util.List;

public class EditionMissionForm {
    private Integer id;
    private String objet, dateDebut, dateFin, observations, statut, chefMissionSel;
    private List<String> membres = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public String getDateDebut() { return dateDebut; }
    public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }
    public String getDateFin() { return dateFin; }
    public void setDateFin(String dateFin) { this.dateFin = dateFin; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getChefMissionSel() { return chefMissionSel; }
    public void setChefMissionSel(String chefMissionSel) { this.chefMissionSel = chefMissionSel; }
    public List<String> getMembres() { return membres; }
    public void setMembres(List<String> membres) { this.membres = membres; }
}
