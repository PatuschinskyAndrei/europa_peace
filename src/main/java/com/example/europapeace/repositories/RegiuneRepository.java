package com.example.europapeace.repositories;

import com.example.europapeace.entities.Regiune;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RegiuneRepository extends JpaRepository<Regiune, Integer> {
    // Metodă pentru a găsi regiunea după numele din data-name (SVG)
    Optional<Regiune> findByNumeIgnoreCase(String nume);
}