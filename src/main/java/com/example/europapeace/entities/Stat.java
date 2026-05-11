package com.example.europapeace.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stat")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Stat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idstat")
    private Integer idstat;

    @Column(name = "nume")
    private String nume;

    @Column(name = "culoare")
    private String culoare;

    @Column(name = "reprezentant")
    private String reprezentant;

    @Column(name = "prefix")
    private String prefix;


    // --- MANUAL GETTERS (in case Lombok is failing in your IDE) ---

    public Integer getIdstat() {
        return idstat;
    }

    public String getNume() {
        return nume;
    }

    public String getCuloare() {
        return culoare;
    }

    public String getReprezentant() {
        return reprezentant;
    }

    public String getPrefix() {
        return prefix;
    }

    // --- MANUAL SETTERS ---

    public void setIdstat(Integer idstat) {
        this.idstat = idstat;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    public void setCuloare(String culoare) {
        this.culoare = culoare;
    }

    public void setReprezentant(String reprezentant) {
        this.reprezentant = reprezentant;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}