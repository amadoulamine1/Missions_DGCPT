package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agent")
@Getter
@Setter
@NoArgsConstructor
public class Agent {

    @Id
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String fonction;
    private String telephone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_agent", nullable = false)
    private TypeAgent typeAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;
}
