package com.example.gestionutilisateur.Repository;

import com.example.gestionutilisateur.Entities.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
}