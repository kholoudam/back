package com.example.gestionutilisateur.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "utilisateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant Keycloak pour synchronisation
    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    private String password;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;

    // 🔹 Relation vers le groupe
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupe_id")
    private Groupe groupe;
}