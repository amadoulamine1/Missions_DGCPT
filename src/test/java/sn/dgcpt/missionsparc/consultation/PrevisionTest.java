package sn.dgcpt.missionsparc.consultation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrevisionTest {

    @Test
    void projette_la_tendance_lineaire() {
        // 10, 20, 30 → pente 10 → année suivante 40
        assertThat(Prevision.projeterAnneeSuivante(List.of(10, 20, 30))).isEqualTo(40.0);
        assertThat(Prevision.projeterEntier(List.of(10, 20, 30))).isEqualTo(40L);
    }

    @Test
    void lisse_le_bruit_par_moindres_carres() {
        // croissance ~+4/an avec bruit : la projection suit la tendance
        Long p = Prevision.projeterEntier(List.of(10, 13, 19, 21));
        assertThat(p).isBetween(23L, 28L);
    }

    @Test
    void borne_a_zero_une_tendance_decroissante() {
        // 30, 20, 10 → pente -10 → -... borné à 0
        assertThat(Prevision.projeterAnneeSuivante(List.of(30, 20, 10))).isEqualTo(0.0);
    }

    @Test
    void serie_plate_renvoie_la_meme_valeur() {
        assertThat(Prevision.projeterAnneeSuivante(List.of(12, 12, 12))).isEqualTo(12.0);
    }

    @Test
    void moins_de_deux_points_pas_de_prevision() {
        assertThat(Prevision.projeterAnneeSuivante(List.of(7))).isNull();
        assertThat(Prevision.projeterAnneeSuivante(List.of())).isNull();
        assertThat(Prevision.projeterEntier(List.of(7))).isNull();
    }
}
