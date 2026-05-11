package com.example.europapeace.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audienta")
public class Audienta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idaudienta;

    private Integer idutilizator;
    private Integer idstat;

    private LocalDateTime dataora;
    private String protocol;
    private String datecontact;

    // NOU: Pentru aprobarea sau respingerea cererii de audienta
    private String status;

    // NOU: Grup sau Privata
    @Column(name = "tip_audienta")
    private String tipAudienta;

    // --- MANUAL GETTERS ---
    public Integer getIdaudienta() { return idaudienta; }
    public Integer getIdutilizator() { return idutilizator; }
    public Integer getIdstat() { return idstat; }
    public LocalDateTime getDataora() { return dataora; }
    public String getProtocol() { return protocol; }
    public String getDatecontact() { return datecontact; }
    public String getStatus() { return status; }
    public String getTipAudienta() { return tipAudienta; }

    // --- MANUAL SETTERS ---
    public void setIdaudienta(Integer idaudienta) { this.idaudienta = idaudienta; }
    public void setIdutilizator(Integer idutilizator) { this.idutilizator = idutilizator; }
    public void setIdstat(Integer idstat) { this.idstat = idstat; }
    public void setDataora(LocalDateTime dataora) { this.dataora = dataora; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public void setDatecontact(String datecontact) { this.datecontact = datecontact; }
    public void setStatus(String status) { this.status = status; }
    public void setTipAudienta(String tipAudienta) { this.tipAudienta = tipAudienta; }
}