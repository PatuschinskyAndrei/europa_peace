package com.example.europapeace.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mesaj_chat", schema = "public")
public class MesajChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_expeditor", nullable = false)
    private Utilizator expeditor;

    // Optional: Pentru soapte directe (folosit rar acum)
    @ManyToOne
    @JoinColumn(name = "id_destinatar", nullable = true)
    private Utilizator destinatar;

    // NOU: Camera privată de care aparține mesajul. (Dacă e null, e Global)
    @ManyToOne
    @JoinColumn(name = "id_audienta", nullable = true)
    private Audienta audienta;

    @Column(name = "mesaj", nullable = false, columnDefinition = "TEXT")
    private String mesaj;

    @Column(name = "data_trimitere", nullable = false)
    private LocalDateTime dataTrimitere;

    // --- MANUAL GETTERS & SETTERS ---
    public Integer getId() { return id; }
    public Utilizator getExpeditor() { return expeditor; }
    public Utilizator getDestinatar() { return destinatar; }
    public Audienta getAudienta() { return audienta; }
    public String getMesaj() { return mesaj; }
    public LocalDateTime getDataTrimitere() { return dataTrimitere; }

    public void setId(Integer id) { this.id = id; }
    public void setExpeditor(Utilizator expeditor) { this.expeditor = expeditor; }
    public void setDestinatar(Utilizator destinatar) { this.destinatar = destinatar; }
    public void setAudienta(Audienta audienta) { this.audienta = audienta; }
    public void setMesaj(String mesaj) { this.mesaj = mesaj; }
    public void setDataTrimitere(LocalDateTime dataTrimitere) { this.dataTrimitere = dataTrimitere; }
}