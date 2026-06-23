package sn.dgcpt.missionsparc.importation.dto;


public class LigneImprimante {
    private int numLigne;
    private String numeroInventaire;
    private String nom;
    private String modele;
    private String mac;
    private String macWifi;
    private String ip;

    public int getNumLigne() { return numLigne; }
    public void setNumLigne(int numLigne) { this.numLigne = numLigne; }
    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public String getMacWifi() { return macWifi; }
    public void setMacWifi(String macWifi) { this.macWifi = macWifi; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
