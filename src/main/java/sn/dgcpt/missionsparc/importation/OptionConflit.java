package sn.dgcpt.missionsparc.importation;

public class OptionConflit {
    private final Integer lotId;
    private final String label, resume;
    public OptionConflit(Integer lotId, String label, String resume) {
        this.lotId = lotId; this.label = label; this.resume = resume;
    }
    public Integer getLotId() { return lotId; }
    public String getLabel() { return label; }
    public String getResume() { return resume; }
}
