package sn.dgcpt.missionsparc.consultation;

import sn.dgcpt.missionsparc.consultation.dto.PageVue;

import java.util.Comparator;
import java.util.List;

public final class Pagination {
    private Pagination() { }
    public static <T> PageVue<T> page(List<T> tout, int page, int size, Comparator<T> tri) {
        List<T> liste = (tri == null) ? tout : tout.stream().sorted(tri).toList();
        int total = liste.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) size));
        int p = Math.max(0, Math.min(page, totalPages - 1));
        int from = Math.min(p * size, total);
        int to = Math.min(from + size, total);
        return new PageVue<>(liste.subList(from, to), p, size, total, totalPages);
    }
}
