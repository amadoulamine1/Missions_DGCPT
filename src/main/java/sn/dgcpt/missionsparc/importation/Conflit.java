package sn.dgcpt.missionsparc.importation;

import java.util.List;

public class Conflit {
    private final String cle, type;
    private final List<OptionConflit> options;
    public Conflit(String cle, String type, List<OptionConflit> options) {
        this.cle = cle; this.type = type; this.options = options;
    }
    public String getCle() { return cle; }
    public String getType() { return type; }
    public List<OptionConflit> getOptions() { return options; }
}
