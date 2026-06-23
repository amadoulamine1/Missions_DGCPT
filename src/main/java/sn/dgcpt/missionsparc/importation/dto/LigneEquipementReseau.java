package sn.dgcpt.missionsparc.importation.dto;


/** Switch ou Access point (onglet "5-Switchs et AP"). */
public class LigneEquipementReseau {
    private int numLigne;
    private String numeroInventaire;
    private String type;   // "Switch" ou "Access point"
    private String nom;
    private String modele;
    private String mac;
    private String ip;

    public int getNumLigne() { return numLigne; }
    public void setNumLigne(int numLigne) { this.numLigne = numLigne; }
    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
