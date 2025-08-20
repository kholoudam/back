package com.example.gestionutilisateur.Entities;

import jakarta.persistence.*;
//import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Province {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    // Relation ManyToOne vers Region (parent)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id")
    @JsonIgnoreProperties("provinces")
    private Region parentRegion;
}