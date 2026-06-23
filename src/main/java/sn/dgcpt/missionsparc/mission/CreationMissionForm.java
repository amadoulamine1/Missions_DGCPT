package sn.dgcpt.missionsparc.mission;

public class CreationMissionForm {
    private Integer posteId;            // TPR existant choisi (null = créer un nouveau)
    private String codePoste, nomPoste; // nouveau TPR
    private String objet, dateDebut, dateFin, chefMission, chefPoste;

    public Integer getPosteId() { return posteId; }
    public void setPosteId(Integer posteId) { this.posteId = posteId; }
    public String getCodePoste() { return codePoste; }
    public void setCodePoste(String codePoste) { this.codePoste = codePoste; }
    public String getNomPoste() { return nomPoste; }
    public void setNomPoste(String nomPoste) { this.nomPoste = nomPoste; }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public String getDateDebut() { return dateDebut; }
    public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }
    public String getDateFin() { return dateFin; }
    public void setDateFin(String dateFin) { this.dateFin = dateFin; }
    public String getChefMission() { return chefMission; }
    public void setChefMission(String chefMission) { this.chefMission = chefMission; }
    public String getChefPoste() { return chefPoste; }
    public void setChefPoste(String chefPoste) { this.chefPoste = chefPoste; }
}
