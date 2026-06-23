package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "logiciel")
@Getter
@Setter
@NoArgsConstructor
public class Logiciel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private boolean actif = true;
}
