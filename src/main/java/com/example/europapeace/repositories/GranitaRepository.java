package com.example.europapeace.repositories;

import com.example.europapeace.entities.Granita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GranitaRepository extends JpaRepository<Granita, Integer> {
    // Permite accesul la datele geometrice ale liniilor de demarcație
}