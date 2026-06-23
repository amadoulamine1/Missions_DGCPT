package sn.dgcpt.missionsparc.importation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.dgcpt.missionsparc.importation.dto.CanevasImporte;

import java.io.IOException;
import java.io.InputStream;

/** Orchestrateur de l'import d'un canevas (cf. Specification-import-canevas.md). */
@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final CanevasReader reader;
    private final ControleImport controle;

    public ImportService(CanevasReader reader, ControleImport controle) {
        this.reader = reader;
        this.controle = controle;
    }

    /** Lecture du canevas (sans rien enregistrer). */
    public CanevasImporte lire(InputStream fichier) throws IOException {
        return reader.lire(fichier);
    }

    /** Contrôles automatiques (formats, champs obligatoires). */
    public RapportImport controler(CanevasImporte canevas) {
        RapportImport rapport = controle.controler(canevas);
        log.info("Canevas mission={} : {} ligne(s), {} bloquant(s), {} avertissement(s)",
                canevas.getEntete().getReference(), rapport.getLignesLues(),
                rapport.nbBloquants(), rapport.nbAvertissements());
        return rapport;
    }

    /**
     * Intégration après validation du chef de mission.
     * Renvoie le nombre de matériels concernés.
     *
     * TODO (cf. spec §6-9) : écriture transactionnelle en base —
     *  rapprochement par numéro d'inventaire puis MAC / numéro de série,
     *  consolidation dans le relevé unique de la mission, attribution des
     *  numéros d'inventaire aux nouveaux matériels, photo datée.
     */
    public int integrer(CanevasImporte canevas) {
        int nbMateriels = canevas.getOrdinateurs().size()
                + canevas.getImprimantes().size()
                + canevas.getEquipementsReseau().size()
                + canevas.getScanners().size();
        log.info("Import validé mission={} : {} matériel(s) prêt(s) à intégrer",
                canevas.getEntete().getReference(), nbMateriels);
        return nbMateriels;
    }
}
