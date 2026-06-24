package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ordinateur")
public class Ordinateur {

    @Id
    private String numeroInventaire;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_inventaire")
    private Materiel materiel;

    @Column(name = "mac_ethernet")
    private String macEthernet;

    @Column(name = "mac_wifi")
    private String macWifi;

    @Column(name = "nom_machine")
    private String nomMachine;

    @Column(name = "ram")
    private String ram;

    @Column(name = "processeur")
    private String processeur;

    @Column(name = "disque_dur")
    private String disqueDur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_installateur_matricule")
    private Agent agentInstallateur;

    @ManyToMany
    @JoinTable(name = "ordinateur_logiciel",
            joinColumns = @JoinColumn(name = "ordinateur_numero"),
            inverseJoinColumns = @JoinColumn(name = "logiciel_id"))
    private Set<Logiciel> logiciels = new HashSet<>();

    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public Materiel getMateriel() { return materiel; }
    public void setMateriel(Materiel materiel) { this.materiel = materiel; }
    public String getMacEthernet() { return macEthernet; }
    public void setMacEthernet(String macEthernet) { this.macEthernet = macEthernet; }
    public String getMacWifi() { return macWifi; }
    public void setMacWifi(String macWifi) { this.macWifi = macWifi; }
    public String getNomMachine() { return nomMachine; }
    public void setNomMachine(String nomMachine) { this.nomMachine = nomMachine; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getProcesseur() { return processeur; }
    public void setProcesseur(String processeur) { this.processeur = processeur; }
    public String getDisqueDur() { return disqueDur; }
    public void setDisqueDur(String disqueDur) { this.disqueDur = disqueDur; }
    public Agent getAgentInstallateur() { return agentInstallateur; }
    public void setAgentInstallateur(Agent agentInstallateur) { this.agentInstallateur = agentInstallateur; }
    public Set<Logiciel> getLogiciels() { return logiciels; }
    public void setLogiciels(Set<Logiciel> logiciels) { this.logiciels = logiciels; }
}
