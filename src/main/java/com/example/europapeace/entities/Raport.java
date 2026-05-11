package com.example.europapeace.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "raport_audienta")
public class Raport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idraport;

    private String titlu;

    @Column(columnDefinition = "TEXT")
    private String continut;

    private LocalDateTime dataGenerare;

    private Boolean estePublic;

    // --- MANUAL GETTERS ---
    public Integer getIdraport() { return idraport; }
    public String getTitlu() { return titlu; }
    public String getContinut() { return continut; }
    public LocalDateTime getDataGenerare() { return dataGenerare; }
    public Boolean getEstePublic() { return estePublic; }

    // --- MANUAL SETTERS (Fixes the red errors in IntelliJ) ---
    public void setIdraport(Integer idraport) { this.idraport = idraport; }
    public void setTitlu(String titlu) { this.titlu = titlu; }
    public void setContinut(String continut) { this.continut = continut; }
    public void setDataGenerare(LocalDateTime dataGenerare) { this.dataGenerare = dataGenerare; }
    public void setEstePublic(Boolean estePublic) { this.estePublic = estePublic; }
}