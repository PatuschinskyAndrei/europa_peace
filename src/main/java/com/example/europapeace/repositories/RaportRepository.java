package com.example.europapeace.repositories;

import com.example.europapeace.entities.Raport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RaportRepository extends JpaRepository<Raport, Integer> {
    // Metodele standard de salvare și căutare sunt generate automat de Spring
}