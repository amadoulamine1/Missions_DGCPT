package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "equipement_reseau")
public class EquipementReseau {

    @Id
    private String numeroInventaire;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_inventaire")
    private Materiel materiel;

    private String mac;
    private String ip;

    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public Materiel getMateriel() { return materiel; }
    public void setMateriel(Materiel materiel) { this.materiel = materiel; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
