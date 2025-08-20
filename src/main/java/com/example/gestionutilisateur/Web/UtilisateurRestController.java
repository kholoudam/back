package com.example.gestionutilisateur.Web;

import com.example.gestionutilisateur.Entities.Utilisateur;
import com.example.gestionutilisateur.KeycloakService;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurRestController {

    private final KeycloakService keycloakService;

    public UtilisateurRestController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    // 🔹 Normalisation string (recherche insensible accents/casse)
    private static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    // ✅ Liste brute des utilisateurs (direct Keycloak)
    @GetMapping("/all")
    public ResponseEntity<List<UserRepresentation>> getAllUtilisateurs() {
        return ResponseEntity.ok(keycloakService.getAllUsers());
    }

    // ✅ Recherche avec filtres (toujours sur Keycloak)
    @GetMapping
    public ResponseEntity<List<UserRepresentation>> searchUtilisateurs(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String regionCode
    ) {
        List<UserRepresentation> result = keycloakService.getAllUsers();

        if (nom != null && !nom.isBlank()) {
            final String q = norm(nom);
            result = result.stream()
                    .filter(u -> {
                        String fn = norm(u.getFirstName());
                        String ln = norm(u.getLastName());
                        String un = norm(u.getUsername());
                        String full = (fn + " " + ln).trim();
                        return fn.contains(q) || ln.contains(q) || un.contains(q) || full.contains(q);
                    })
                    .collect(Collectors.toList());
        }

        if (username != null && !username.isBlank()) {
            final String q = norm(username);
            result = result.stream()
                    .filter(u -> norm(u.getUsername()).contains(q))
                    .collect(Collectors.toList());
        }

        if (email != null && !email.isBlank()) {
            final String q = norm(email);
            result = result.stream()
                    .filter(u -> norm(u.getEmail()).contains(q))
                    .collect(Collectors.toList());
        }

        if (regionCode != null && !regionCode.isBlank()) {
            final String q = norm(regionCode);
            result = result.stream()
                    .filter(u -> keycloakService.getUserGroups(u.getId()).stream()
                            .anyMatch(g -> norm(g.getName()).equals(q)))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(result);
    }

    // ✅ Créer utilisateur (dans Keycloak)
    @PostMapping
    public ResponseEntity<?> createUtilisateur(@RequestBody Utilisateur utilisateur) {
        try {
            String id = keycloakService.createUserInKeycloak(utilisateur);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Utilisateur créé avec succès", "id", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Mise à jour utilisateur
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUtilisateur(@PathVariable String id, @RequestBody Utilisateur utilisateur) {
        try {
            utilisateur.setKeycloakId(id); // on injecte l'id dans l'objet
            keycloakService.updateUserInKeycloak(utilisateur);
            return ResponseEntity.ok(Map.of("message", "Utilisateur mis à jour"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Récupération d’un utilisateur
    @GetMapping("/{id}")
    public ResponseEntity<UserRepresentation> getUtilisateurById(@PathVariable String id) {
        return keycloakService.getAllUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Affectation utilisateur à une région (= groupe Keycloak)
    @PostMapping("/affecter-region")
    public ResponseEntity<?> affecterUtilisateurRegion(
            @RequestParam String utilisateurId,
            @RequestParam String regionCode
    ) {
        try {
            keycloakService.affecterUtilisateurAuGroupeRegion(utilisateurId, regionCode);
            return ResponseEntity.ok(Map.of("message", "Utilisateur affecté à la région"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}