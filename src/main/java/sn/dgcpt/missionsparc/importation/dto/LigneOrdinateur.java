package sn.dgcpt.missionsparc.importation.dto;


public class LigneOrdinateur {
    private int numLigne;
    private String numeroInventaire;
    private String nomMachine;
    private String modele;
    private String macEthernet;
    private String macWifi;
    private String agentAttributaire;
    private String agentInstallateur;
    private boolean aster;
    private boolean antivirus;
    private boolean sicCDD;
    private boolean cic;
    private boolean sysbudget;
    private boolean ad;
    private String ram;
    private String processeur;
    private String disqueDur;
    private String statut;
    private String observation;

    public int getNumLigne() { return numLigne; }
    public void setNumLigne(int numLigne) { this.numLigne = numLigne; }
    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public String getNomMachine() { return nomMachine; }
    public void setNomMachine(String nomMachine) { this.nomMachine = nomMachine; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public String getMacEthernet() { return macEthernet; }
    public void setMacEthernet(String macEthernet) { this.macEthernet = macEthernet; }
    public String getMacWifi() { return macWifi; }
    public void setMacWifi(String macWifi) { this.macWifi = macWifi; }
    public String getAgentAttributaire() { return agentAttributaire; }
    public void setAgentAttributaire(String agentAttributaire) { this.agentAttributaire = agentAttributaire; }
    public String getAgentInstallateur() { return agentInstallateur; }
    public void setAgentInstallateur(String agentInstallateur) { this.agentInstallateur = agentInstallateur; }
    public boolean isAster() { return aster; }
    public void setAster(boolean aster) { this.aster = aster; }
    public boolean isAntivirus() { return antivirus; }
    public void setAntivirus(boolean antivirus) { this.antivirus = antivirus; }
    public boolean isSicCDD() { return sicCDD; }
    public void setSicCDD(boolean sicCDD) { this.sicCDD = sicCDD; }
    public boolean isCic() { return cic; }
    public void setCic(boolean cic) { this.cic = cic; }
    public boolean isSysbudget() { return sysbudget; }
    public void setSysbudget(boolean sysbudget) { this.sysbudget = sysbudget; }
    public boolean isAd() { return ad; }
    public void setAd(boolean ad) { this.ad = ad; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getProcesseur() { return processeur; }
    public void setProcesseur(String processeur) { this.processeur = processeur; }
    public String getDisqueDur() { return disqueDur; }
    public void setDisqueDur(String disqueDur) { this.disqueDur = disqueDur; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
}
