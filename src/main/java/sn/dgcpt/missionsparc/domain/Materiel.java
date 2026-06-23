package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "materiel")
@Getter
@Setter
@NoArgsConstructor
public class Materiel {

    @Id
    @Column(name = "numero_inventaire")
    private String numeroInventaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMateriel type;

    private String nom;
    private String modele;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    @Column(name = "date_creation")
    private Instant dateCreation;
}
