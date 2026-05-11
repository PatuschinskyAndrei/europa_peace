package com.example.europapeace.repositories;

import com.example.europapeace.entities.MesajChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MesajChatRepository extends JpaRepository<MesajChat, Integer> {
    List<MesajChat> findAllByOrderByDataTrimitereAsc(); // Extrage mesajele in ordine cronologica
}