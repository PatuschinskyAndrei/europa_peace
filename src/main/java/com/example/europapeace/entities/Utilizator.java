package com.example.europapeace.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilizator", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Utilizator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idutilizator")
    private Integer id;

    @Column(name = "numeutilizator", nullable = false, length = 50)
    private String username;

    @Column(name = "parola", nullable = false, length = 255)
    private String parola;

    @Column(name = "idrol")
    private Integer idrol;

    @Column(name = "idstat")
    private Integer idstat;

    @Column(name = "email", length = 255)
    private String email;

    // --- MANUAL GETTERS ---
    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getParola() { return parola; }
    public Integer getIdrol() { return idrol; }
    public Integer getIdstat() { return idstat; }
    public String getEmail() { return email; }

    // --- MANUAL SETTERS PENTRU ADMIN PANEL ---
    public void setId(Integer id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setParola(String parola) { this.parola = parola; }
    public void setIdrol(Integer idrol) { this.idrol = idrol; }
    public void setIdstat(Integer idstat) { this.idstat = idstat; }
    public void setEmail(String email) { this.email = email; }
}