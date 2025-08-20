package com.example.gestionutilisateur.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gestionutilisateur.Entities.Groupe;

import java.util.Optional;

public interface GroupeRepository extends JpaRepository<Groupe, Long> {
    Optional<Groupe> findByLabel(String label);

    // 🔹 Nouvelle méthode pour rechercher un groupe par son code
    Optional<Groupe> findByCode(String code);

    String code(String code);
}