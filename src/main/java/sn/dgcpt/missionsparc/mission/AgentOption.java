package sn.dgcpt.missionsparc.mission;

public class AgentOption {
    private final String matricule, libelle;
    public AgentOption(String matricule, String libelle) { this.matricule = matricule; this.libelle = libelle; }
    public String getMatricule() { return matricule; }
    public String getLibelle() { return libelle; }
}
