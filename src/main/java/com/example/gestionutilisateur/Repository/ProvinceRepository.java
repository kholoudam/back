package com.example.gestionutilisateur.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gestionutilisateur.Entities.Province;

public interface ProvinceRepository extends JpaRepository<Province, Long>
{

}