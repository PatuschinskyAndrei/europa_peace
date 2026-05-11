package com.example.europapeace.services;

import com.example.europapeace.entities.Regiune;
import com.example.europapeace.repositories.RegiuneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MapService {

    @Autowired
    private RegiuneRepository regiuneRepository;

    public List<Regiune> obtineToateRegiunile() {
        // În loc de state.getFormagrafica(), acum returnăm regiunile care au desenul
        return regiuneRepository.findAll();
    }
}