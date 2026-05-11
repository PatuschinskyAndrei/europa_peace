package com.example.europapeace.config;

import com.example.europapeace.entities.Utilizator;

public class UserSession {
    private static UserSession instance;
    private Utilizator utilizatorLogat;

    // Constructor privat pentru a preveni crearea de noi instanțe cu 'new'
    private UserSession() {}

    // Metoda prin care accesezi sesiunea de oriunde
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setUtilizatorLogat(Utilizator utilizator) {
        this.utilizatorLogat = utilizator;
    }

    public Utilizator getUtilizatorLogat() {
        return utilizatorLogat;
    }

    public void cleanUserSession() {
        utilizatorLogat = null;
    }
}