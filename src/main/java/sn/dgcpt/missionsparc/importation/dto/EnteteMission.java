package sn.dgcpt.missionsparc.importation.dto;


/** En-tête lue dans l'onglet "1-Mission et Réseau" du canevas. */
public class EnteteMission {
    private String reference;          // N° de mission
    private String codePoste;
    private String nomPoste;
    private String objet;
    private String dateDebut;          // brut (JJ/MM/AAAA)
    private String dateFin;
    private String chefMission;        // matricule
    private String chefPoste;          // matricule
    private String agentSaisisseur;    // matricule de celui qui remplit CE fichier
    private String zone;
    private String etatCablage;
    private String categorieCable;

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
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
    public String getAgentSaisisseur() { return agentSaisisseur; }
    public void setAgentSaisisseur(String agentSaisisseur) { this.agentSaisisseur = agentSaisisseur; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getEtatCablage() { return etatCablage; }
    public void setEtatCablage(String etatCablage) { this.etatCablage = etatCablage; }
    public String getCategorieCable() { return categorieCable; }
    public void setCategorieCable(String categorieCable) { this.categorieCable = categorieCable; }
}
