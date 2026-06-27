package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

public class MissionVue {
    private final Integer id;
    private final String reference, objet, posteNom, periode, statut, etat, agents;
    private final List<OrdreLien> ordres;
    public MissionVue(Integer id, String reference, String objet, String posteNom, String periode, String statut, String etat,
                      String agents, List<OrdreLien> ordres) {
        this.id = id; this.reference = reference; this.objet = objet; this.posteNom = posteNom; this.periode = periode;
        this.statut = statut; this.etat = etat; this.agents = agents;
        this.ordres = (ordres == null) ? List.of() : ordres;
    }
    public Integer getId() { return id; }
    public String getReference() { return reference; }
    public String getObjet() { return objet; }
    public String getPosteNom() { return posteNom; }
    public String getPeriode() { return periode; }
    public String getStatut() { return statut; }
    public String getEtat() { return etat; }
    /** Agents informaticiens en charge (chef de mission en tête, suivi des membres). */
    public String getAgents() { return agents; }
    /** Ordres de mission (PDF) attachés ; liste vide s'il n'y en a pas. */
    public List<OrdreLien> getOrdres() { return ordres; }
}
