package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

/** En-tête lue dans l'onglet "1-Mission et Réseau" du canevas. */
@Getter
@Setter
public class EnteteMission {
    private String reference;          // N° de mission
    private String codePoste;
    private String nomPoste;
    private String objet;
    private String dateDebut;          // brut (JJ/MM/AAAA)
    private String dateFin;
    private String chefMission;        // matricule
    private String chefPoste;          // matricule
    private String agentSaisisseur;    // matricule de celui qui remplit CE fichier
    private String zone;
    private String etatCablage;
    private String categorieCable;
}
