package com.example.europapeace.repositories;

import com.example.europapeace.entities.Stat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StatRepository extends JpaRepository<Stat, Integer> {

    // Această linie rezolvă eroarea din imaginea ta:
    Optional<Stat> findByNume(String nume);

}