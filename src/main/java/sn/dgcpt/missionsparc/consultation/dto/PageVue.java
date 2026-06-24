package sn.dgcpt.missionsparc.consultation.dto;

import java.util.List;

public class PageVue<T> {
    private final List<T> contenu;
    private final int page, size, total, totalPages;
    public PageVue(List<T> contenu, int page, int size, int total, int totalPages) {
        this.contenu = contenu; this.page = page; this.size = size; this.total = total; this.totalPages = totalPages;
    }
    public List<T> getContenu() { return contenu; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
    public int getPageHumain() { return page + 1; }
    public boolean isPremier() { return page <= 0; }
    public boolean isDernier() { return page >= totalPages - 1; }
    public int getPrecedent() { return Math.max(0, page - 1); }
    public int getSuivant() { return Math.min(totalPages - 1, page + 1); }
    public boolean isVide() { return contenu == null || contenu.isEmpty(); }
}
