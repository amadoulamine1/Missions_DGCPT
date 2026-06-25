package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

public class PosteDetailVue {
    private final PosteVue poste;
    private final String chefActuel;
    private final String chefActuelMatricule;
    private final List<AgentVue> agents;
    private final List<MaterielLignePoste> materiels;
    private final List<String[]> missions;
    private final List<String[]> fichiers;
    private final List<String[]> affectations;
    private final List<String[]> chefsPoste;

    public PosteDetailVue(PosteVue poste, String chefActuel, String chefActuelMatricule,
                          List<AgentVue> agents, List<MaterielLignePoste> materiels,
                          List<String[]> missions, List<String[]> fichiers,
                          List<String[]> affectations, List<String[]> chefsPoste) {
        this.poste = poste; this.chefActuel = chefActuel; this.chefActuelMatricule = chefActuelMatricule;
        this.agents = agents; this.materiels = materiels;
        this.missions = missions; this.fichiers = fichiers;
        this.affectations = affectations; this.chefsPoste = chefsPoste;
    }
    public PosteVue getPoste() { return poste; }
    public String getChefActuel() { return chefActuel; }
    public String getChefActuelMatricule() { return chefActuelMatricule; }
    public List<AgentVue> getAgents() { return agents; }
    public List<MaterielLignePoste> getMateriels() { return materiels; }
    /** Historique des missions du poste : [référence, objet, période, état]. */
    public List<String[]> getMissions() { return missions; }
    /** Fichiers (canevas) chargés : [mission, fichier, saisisseur, date, statut]. */
    public List<String[]> getFichiers() { return fichiers; }
    /** Historique des affectations de matériel : [n° inventaire, matériel, agent, depuis, jusqu'à]. */
    public List<String[]> getAffectations() { return affectations; }
    /** Historique des chefs de poste : [agent, depuis, jusqu'à]. */
    public List<String[]> getChefsPoste() { return chefsPoste; }
}
