package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

public class MissionDetailVue {
    private final MissionVue mission;
    private final String chefMission, chefPoste, etatCablage, categorieCable;
    private final List<ReleveVue> releves;
    private final List<AgentVue> membres;
    public MissionDetailVue(MissionVue mission, String chefMission, String chefPoste, String etatCablage,
                            String categorieCable, List<ReleveVue> releves, List<AgentVue> membres) {
        this.mission = mission; this.chefMission = chefMission; this.chefPoste = chefPoste;
        this.etatCablage = etatCablage; this.categorieCable = categorieCable; this.releves = releves; this.membres = membres;
    }
    public MissionVue getMission() { return mission; }
    public String getChefMission() { return chefMission; }
    public String getChefPoste() { return chefPoste; }
    public String getEtatCablage() { return etatCablage; }
    public String getCategorieCable() { return categorieCable; }
    public List<ReleveVue> getReleves() { return releves; }
    public List<AgentVue> getMembres() { return membres; }
}
