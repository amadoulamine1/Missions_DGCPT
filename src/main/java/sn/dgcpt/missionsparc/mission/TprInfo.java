package sn.dgcpt.missionsparc.mission;

import java.util.List;

public class TprInfo {
    private final List<AgentOption> agents;
    private final String dernierChef;
    public TprInfo(List<AgentOption> agents, String dernierChef) { this.agents = agents; this.dernierChef = dernierChef; }
    public List<AgentOption> getAgents() { return agents; }
    public String getDernierChef() { return dernierChef; }
}
