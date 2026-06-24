package sn.dgcpt.missionsparc;

import org.junit.jupiter.api.Test;
import sn.dgcpt.missionsparc.consultation.StatsExporter;
import sn.dgcpt.missionsparc.consultation.dto.StatPoste;
import sn.dgcpt.missionsparc.domain.Materiel;
import sn.dgcpt.missionsparc.domain.Poste;
import sn.dgcpt.missionsparc.domain.StatutMateriel;
import sn.dgcpt.missionsparc.domain.TypeMateriel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsExporterTest {

    private Poste poste(int id, String nom) {
        Poste p = new Poste(); p.setId(id); p.setNom(nom); return p;
    }

    private Materiel mat(String num, Poste poste, StatutMateriel statut) {
        Materiel m = new Materiel();
        m.setNumeroInventaire(num);
        m.setType(TypeMateriel.ORDINATEUR);
        m.setPoste(poste);
        m.setStatut(statut);
        return m;
    }

    @Test
    void parPoste_agrege_trie_par_total_et_compte_les_pannes() {
        Poste dkr = poste(1, "Dakar");
        Poste ths = poste(2, "Thiès");
        List<Materiel> parc = List.of(
                mat("A1", dkr, StatutMateriel.EN_SERVICE),
                mat("A2", dkr, StatutMateriel.EN_PANNE),
                mat("A3", dkr, StatutMateriel.EN_SERVICE),
                mat("B1", ths, StatutMateriel.EN_SERVICE)
        );

        List<StatPoste> res = StatsExporter.parPoste(parc);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getNom()).isEqualTo("Dakar");
        assertThat(res.get(0).getTotal()).isEqualTo(3);
        assertThat(res.get(0).getEnPanne()).isEqualTo(1);
        assertThat(res.get(1).getNom()).isEqualTo("Thiès");
        assertThat(res.get(1).getTotal()).isEqualTo(1);
        assertThat(res.get(1).getEnPanne()).isEqualTo(0);
    }

    @Test
    void parPoste_regroupe_le_materiel_sans_poste() {
        List<Materiel> parc = List.of(mat("X1", null, StatutMateriel.EN_SERVICE));
        List<StatPoste> res = StatsExporter.parPoste(parc);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getNom()).isEqualTo("(sans poste)");
        assertThat(res.get(0).getTotal()).isEqualTo(1);
    }
}
