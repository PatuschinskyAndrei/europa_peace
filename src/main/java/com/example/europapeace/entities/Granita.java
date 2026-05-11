package com.example.europapeace.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "granita")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Granita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idgranita;
    private Integer idstat;
    @Column(columnDefinition = "TEXT")
    private String punctecurbe;
    private Double lungime;
}