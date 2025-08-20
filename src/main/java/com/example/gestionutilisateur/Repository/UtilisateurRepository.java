package com.example.gestionutilisateur.Repository;

import com.example.gestionutilisateur.Entities.Groupe;
import com.example.gestionutilisateur.Entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    List<Utilisateur> findByUsernameContainingIgnoreCase(String username);
    List<Utilisateur> findByEmailContainingIgnoreCase(String email);
    List<Utilisateur> findByGroupe_LabelIgnoreCase(String groupeLabel);
    List<Utilisateur> findByGroupe(Groupe groupe);
    Optional<Utilisateur> findByKeycloakId(String keycloakId);
}