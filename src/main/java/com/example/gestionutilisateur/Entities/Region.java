package com.example.gestionutilisateur.Entities;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Getter
@Setter
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    // Relation OneToMany vers Province
    @OneToMany(mappedBy = "parentRegion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("parentRegion")
    private List<Province> provinces;

}