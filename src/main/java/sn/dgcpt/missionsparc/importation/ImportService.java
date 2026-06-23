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

    /** Étapes 4-5 de la spec : lecture du canevas puis contrôles automatiques. */
    public RapportImport analyser(InputStream fichier) throws IOException {
        CanevasImporte canevas = reader.lire(fichier);
        RapportImport rapport = controle.controler(canevas);
        log.info("Canevas mission={} : {} ligne(s) lue(s), {} bloquant(s), {} avertissement(s)",
                canevas.getEntete().getReference(), rapport.getLignesLues(),
                rapport.nbBloquants(), rapport.nbAvertissements());
        return rapport;
    }

    // TODO étapes 6-9 de la spec :
    //  - rapprochement par numéro d'inventaire, sinon MAC / numéro de série ;
    //  - consolidation dans le relevé unique de la mission (statut EN_CONSOLIDATION) ;
    //  - arbitrage des doublons / conflits par le chef de mission ;
    //  - intégration transactionnelle : matériel + sous-type, affectations historisées,
    //    relevés (photo datée), attribution des numéros d'inventaire aux nouveaux matériels.
}
