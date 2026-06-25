package sn.dgcpt.missionsparc.importation.dto;


import java.util.ArrayList;
import java.util.List;

/** Contenu complet d'un canevas chargé (un fichier d'un agent). */
public class CanevasImporte {
    private EnteteMission entete = new EnteteMission();
    private List<LigneMembre> membres = new ArrayList<>();
    private List<LigneOrdinateur> ordinateurs = new ArrayList<>();
    private List<LigneImprimante> imprimantes = new ArrayList<>();
    private List<LigneEquipementReseau> equipementsReseau = new ArrayList<>();
    private List<LigneScanner> scanners = new ArrayList<>();
    private List<LigneAutreMateriel> autres = new ArrayList<>();
    private List<LigneAgentPoste> agentsTpr = new ArrayList<>();

    public EnteteMission getEntete() { return entete; }
    public void setEntete(EnteteMission entete) { this.entete = entete; }
    public List<LigneMembre> getMembres() { return membres; }
    public void setMembres(List<LigneMembre> membres) { this.membres = membres; }
    public List<LigneOrdinateur> getOrdinateurs() { return ordinateurs; }
    public void setOrdinateurs(List<LigneOrdinateur> ordinateurs) { this.ordinateurs = ordinateurs; }
    public List<LigneImprimante> getImprimantes() { return imprimantes; }
    public void setImprimantes(List<LigneImprimante> imprimantes) { this.imprimantes = imprimantes; }
    public List<LigneEquipementReseau> getEquipementsReseau() { return equipementsReseau; }
    public void setEquipementsReseau(List<LigneEquipementReseau> equipementsReseau) { this.equipementsReseau = equipementsReseau; }
    public List<LigneScanner> getScanners() { return scanners; }
    public void setScanners(List<LigneScanner> scanners) { this.scanners = scanners; }
    public List<LigneAutreMateriel> getAutres() { return autres; }
    public void setAutres(List<LigneAutreMateriel> autres) { this.autres = autres; }
    public List<LigneAgentPoste> getAgentsTpr() { return agentsTpr; }
    public void setAgentsTpr(List<LigneAgentPoste> agentsTpr) { this.agentsTpr = agentsTpr; }
}
