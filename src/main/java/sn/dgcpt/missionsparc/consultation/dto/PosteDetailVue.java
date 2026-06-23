package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

public class PosteDetailVue {
    private final PosteVue poste;
    private final String chefActuel;
    private final List<AgentVue> agents;
    private final List<MaterielVue> materiels;
    public PosteDetailVue(PosteVue poste, String chefActuel, List<AgentVue> agents, List<MaterielVue> materiels) {
        this.poste = poste; this.chefActuel = chefActuel; this.agents = agents; this.materiels = materiels;
    }
    public PosteVue getPoste() { return poste; }
    public String getChefActuel() { return chefActuel; }
    public List<AgentVue> getAgents() { return agents; }
    public List<MaterielVue> getMateriels() { return materiels; }
}
