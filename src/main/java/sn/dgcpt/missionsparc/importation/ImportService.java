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
    private final IntegrationService integration;

    public ImportService(CanevasReader reader, ControleImport controle, IntegrationService integration) {
        this.reader = reader;
        this.controle = controle;
        this.integration = integration;
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

    /** Intégration en base après validation. Renvoie le nombre de matériels traités. */
    public int integrer(CanevasImporte canevas) {
        return integration.integrer(canevas);
    }
}
