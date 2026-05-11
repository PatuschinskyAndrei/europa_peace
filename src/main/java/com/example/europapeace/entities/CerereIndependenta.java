package com.example.europapeace.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cerere_independenta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CerereIndependenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idcerere")
    private Integer idcerere;

    // The database demands this is NOT NULL (Blue toggle ON)
    @Column(name = "idrol", nullable = false)
    private Integer idrol = 1; // Default to 1 so the DB is happy

    // The database demands this is NOT NULL (Blue toggle ON)
    @Column(name = "idstat", nullable = false)
    private Integer idstat;

    // The database demands this is NOT NULL (Blue toggle ON)
    // We map your Java variable to "numegrup" to fix the crash
    @Column(name = "numegrup", length = 100, nullable = false)
    private String numeGrupEmitent;

    // The database demands this is NOT NULL (Blue toggle ON)
    @Column(name = "categorie", length = 50, nullable = false)
    private String categorie;

    // The database demands this is NOT NULL (Blue toggle ON)
    @ManyToOne
    @JoinColumn(name = "idutilizator", nullable = false)
    private Utilizator sefDeStat;

    // --- Nullable Columns (Grey toggles) ---

    @Column(name = "nume_cerere", length = 100)
    private String numeCerere;

    @Column(name = "continut", columnDefinition = "TEXT")
    private String continut;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "data_introducerii")
    private LocalDateTime dataIntroducerii;

    // --- MANUAL GETTERS & SETTERS ---
    public Integer getIdcerere() { return idcerere; }
    public Integer getIdrol() { return idrol; }
    public Integer getIdstat() { return idstat; }
    public String getNumeGrupEmitent() { return numeGrupEmitent; }
    public String getCategorie() { return categorie; }
    public Utilizator getSefDeStat() { return sefDeStat; }
    public String getNumeCerere() { return numeCerere; }
    public String getContinut() { return continut; }
    public String getStatus() { return status; }
    public LocalDateTime getDataIntroducerii() { return dataIntroducerii; }

    public void setIdcerere(Integer idcerere) { this.idcerere = idcerere; }
    public void setIdrol(Integer idrol) { this.idrol = idrol; }
    public void setIdstat(Integer idstat) { this.idstat = idstat; }
    public void setNumeGrupEmitent(String numeGrupEmitent) { this.numeGrupEmitent = numeGrupEmitent; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public void setSefDeStat(Utilizator sefDeStat) { this.sefDeStat = sefDeStat; }
    public void setNumeCerere(String numeCerere) { this.numeCerere = numeCerere; }
    public void setContinut(String continut) { this.continut = continut; }
    public void setStatus(String status) { this.status = status; }
    public void setDataIntroducerii(LocalDateTime dataIntroducerii) { this.dataIntroducerii = dataIntroducerii; }
}