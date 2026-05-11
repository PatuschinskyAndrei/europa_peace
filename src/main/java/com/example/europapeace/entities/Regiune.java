package com.example.europapeace.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regiune")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Regiune {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idregiune;

    private String nume;

    @Column(columnDefinition = "TEXT")
    private String formagrafica;

    private Boolean esteindependenta;

    @ManyToOne
    @JoinColumn(name = "idstat")
    private Stat stat;

    public String getNume() { return nume; }
    public String getFormagrafica() { return formagrafica; }
    public Stat getStat() { return stat; }
    public void setStat(Stat stat) { this.stat = stat; }
}