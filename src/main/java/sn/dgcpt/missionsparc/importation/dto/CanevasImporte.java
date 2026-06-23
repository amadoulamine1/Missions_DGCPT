package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Contenu complet d'un canevas chargé (un fichier d'un agent). */
@Getter
@Setter
public class CanevasImporte {
    private EnteteMission entete = new EnteteMission();
    private List<LigneMembre> membres = new ArrayList<>();
    private List<LigneOrdinateur> ordinateurs = new ArrayList<>();
    private List<LigneImprimante> imprimantes = new ArrayList<>();
    private List<LigneEquipementReseau> equipementsReseau = new ArrayList<>();
    private List<LigneScanner> scanners = new ArrayList<>();
}
