package com.example.gestionutilisateur;

// import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean;

// import com.example.gestionutilisateur.Repository.UtilisateurRepository;
// import com.example.gestionutilisateur.Entities.Groupe;
// import com.example.gestionutilisateur.Entities.Province;
// import com.example.gestionutilisateur.Entities.Region;
// import com.example.gestionutilisateur.Entities.Utilisateur;
// import com.example.gestionutilisateur.Repository.GroupeRepository;
// import com.example.gestionutilisateur.Repository.RegionRepository;
// import com.example.gestionutilisateur.Repository.ProvinceRepository;

// import java.util.Arrays;
// import java.util.Date;
// import java.util.List;

@SpringBootApplication
public class GestionUtilisateurApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionUtilisateurApplication.class, args);
    }
//     @Bean
//     CommandLineRunner start(
//             RegionRepository regionRepo,
//             ProvinceRepository provinceRepo,
//             GroupeRepository groupeRepo,
//             UtilisateurRepository utilisateurRepo) {

//         return args -> {

//             // 1. Ajouter les régions
//             Region r1 = new Region();
//             r1.setNom("Région Casablanca-Settat");
//             regionRepo.save(r1);

//             Region r2 = new Region();
//             r2.setNom("Région Marrakech-Safi");
//             regionRepo.save(r2);

//             // 2. Ajouter les provinces
//             Province p1 = new Province();
//             p1.setNom("Casablanca");
//             p1.setParentRegion(r1);
//             provinceRepo.save(p1);

//             Province p2 = new Province();
//             p2.setNom("Settat");
//             p2.setParentRegion(r1);
//             provinceRepo.save(p2);

//             Province p3 = new Province();
//             p3.setNom("Marrakech");
//             p3.setParentRegion(r2);
//             provinceRepo.save(p3);

//             // 3. Générer dynamiquement les groupes à partir des provinces
//             List<Province> provinces = provinceRepo.findAll();
//             int counter = 1;

//             for (Province province : provinces) {
//                 Groupe groupe = new Groupe();
//                 groupe.setCode(String.format("%02d", counter));
//                 String regionNom = province.getParentRegion() != null ? province.getParentRegion().getNom() : "Inconnu";
//                 groupe.setLabel(regionNom + " - " + province.getNom());
//                 groupeRepo.save(groupe);
//                 counter++;
//             }

//             // 4. Ajouter les utilisateurs
//             Utilisateur u1 = Utilisateur.builder()
//                     .username("kholoud")
//                     .email("kholoud@example.com")
//                     .password("password123")
//                     .firstName("Kholoud")
//                     .lastName("Allam")
//                     .phoneNumber("0600000000")
//                     .address("Casablanca")
//                     .province(p1)
//                     .createdAt(new Date())
//                     .status("ACTIF")
//                     .build();

//             Utilisateur u2 = Utilisateur.builder()
//                     .username("youssef")
//                     .email("youssef@example.com")
//                     .password("password123")
//                     .firstName("Youssef")
//                     .lastName("Benali")
//                     .phoneNumber("0611111111")
//                     .address("Settat")
//                     .province(p2)
//                     .createdAt(new Date())
//                     .status("ACTIF")
//                     .build();

//             utilisateurRepo.saveAll(List.of(u1, u2));
//         };
//     }
}