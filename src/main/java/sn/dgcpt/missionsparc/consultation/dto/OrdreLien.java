package sn.dgcpt.missionsparc.consultation.dto;

/** Lien vers un ordre de mission (PDF) attaché : identifiant + nom de fichier. */
public class OrdreLien {
    private final Integer id;
    private final String nom;
    public OrdreLien(Integer id, String nom) { this.id = id; this.nom = nom; }
    public Integer getId() { return id; }
    public String getNom() { return nom; }
}
