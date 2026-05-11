package com.example.europapeace.repositories;

import com.example.europapeace.entities.Audienta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AudientaRepository extends JpaRepository<Audienta, Integer> {
    // Găsește audiențele unui anumit stat
    List<Audienta> findByIdstat(Integer idstat);
}