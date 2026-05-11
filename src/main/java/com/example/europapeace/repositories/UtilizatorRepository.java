package com.example.europapeace.repositories;

import com.example.europapeace.entities.Utilizator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UtilizatorRepository extends JpaRepository<Utilizator, Integer> {
    // Această metodă va căuta în coloana 'username' din tabelă
    Utilizator findByUsername(String username);
}