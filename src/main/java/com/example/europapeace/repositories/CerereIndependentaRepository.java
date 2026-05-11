package com.example.europapeace.repositories;

import com.example.europapeace.entities.CerereIndependenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CerereIndependentaRepository extends JpaRepository<CerereIndependenta, Integer> {

    // Finds requests by their category (e.g., "Cerere Etnică")
    List<CerereIndependenta> findByCategorie(String categorie);

    // Searches for a group by name (ignoring uppercase/lowercase)
    List<CerereIndependenta> findByNumeGrupEmitentContainingIgnoreCase(String numeGrup);
}