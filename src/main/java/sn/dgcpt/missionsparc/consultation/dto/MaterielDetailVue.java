package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

public class MaterielDetailVue {
    private final MaterielVue base;
    private final String dateCreation, affecteA, affectePoste, affecteDepuis;
    private final List<String[]> attributs;
    private final List<String[]> releves;
    private final List<String[]> historique;

    public MaterielDetailVue(MaterielVue base, String dateCreation, List<String[]> attributs,
                             String affecteA, String affectePoste, String affecteDepuis,
                             List<String[]> releves, List<String[]> historique) {
        this.base = base; this.dateCreation = dateCreation; this.attributs = attributs;
        this.affecteA = affecteA; this.affectePoste = affectePoste; this.affecteDepuis = affecteDepuis;
        this.releves = releves; this.historique = historique;
    }
    public MaterielVue getBase() { return base; }
    public String getDateCreation() { return dateCreation; }
    public List<String[]> getAttributs() { return attributs; }
    public String getAffecteA() { return affecteA; }
    public String getAffectePoste() { return affectePoste; }
    public String getAffecteDepuis() { return affecteDepuis; }
    public List<String[]> getReleves() { return releves; }
    public List<String[]> getHistorique() { return historique; }
}
