package com.example.gestionutilisateur.Web;

import com.example.gestionutilisateur.Entities.Groupe;
import com.example.gestionutilisateur.KeycloakService;
import com.example.gestionutilisateur.Repository.GroupeRepository;
import com.example.gestionutilisateur.Repository.UtilisateurRepository;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groupes")
@CrossOrigin(origins = "*")
public class GroupeRestController {

    private final KeycloakService keycloakService;
    private final UtilisateurRepository utilisateurRepository;
    private final GroupeRepository groupeRepository;

    public GroupeRestController(KeycloakService keycloakService, UtilisateurRepository utilisateurRepository, GroupeRepository groupeRepository) {
        this.keycloakService = keycloakService;
        this.utilisateurRepository = utilisateurRepository;
        this.groupeRepository = groupeRepository;
    }

    // 🔹 Récupère tous les groupes avec hiérarchie (code + label + enfants)
    @GetMapping("/hierarchy")
    public ResponseEntity<List<Map<String, Object>>> getGroupHierarchy() {
        try {
            List<Map<String, Object>> hierarchy = keycloakService.getGroupHierarchy();
            return ResponseEntity.ok(hierarchy);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // 🔹 Création d’un groupe racine
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            String type = body.getOrDefault("type", "region");
            String code = body.getOrDefault("code", "");
            String nom = body.getOrDefault("nom", name);

            String id = keycloakService.createGroup(name, type, code, nom);
            return ResponseEntity.ok(Map.of(
                    "message", "Groupe créé avec succès",
                    "id", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Erreur lors de la création du groupe",
                    "details", e.getMessage()
            ));
        }
    }

    // 🔹 Mise à jour d’un groupe
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable String groupId, @RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            String type = body.get("type");
            String code = body.get("code");
            String nom = body.get("nom");

            keycloakService.updateGroupAttributes(groupId, name, type, code, nom);
            return ResponseEntity.ok(Map.of("message", "Groupe mis à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Erreur lors de la mise à jour du groupe",
                    "details", e.getMessage()
            ));
        }
    }

    // 🔹 Ajouter un sous-groupe à un groupe parent
    @PostMapping("/{parentId}/children")
    public ResponseEntity<?> addChildGroup(@PathVariable String parentId, @RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            String type = body.getOrDefault("type", "region");
            String code = body.getOrDefault("code", "");
            String nom = body.getOrDefault("nom", name);

            keycloakService.addChildGroup(parentId, name, type, code, nom);
            return ResponseEntity.ok(Map.of("message", "Sous-groupe créé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Erreur lors de la création du sous-groupe",
                    "details", e.getMessage()
            ));
        }
    }

    // 🔹 Récupérer tous les groupes pour dropdown avec utilisateurs
    @GetMapping("/dropdown")
    public ResponseEntity<List<Map<String, Object>>> getGroupesDropdown() {
        try {
            List<Map<String, Object>> keycloakGroups = keycloakService.getAllGroupsFromKeycloak();

            AtomicInteger counter = new AtomicInteger(1);

            List<Map<String, Object>> result = keycloakGroups.stream().map(g -> {
                Map<String, Object> map = new HashMap<>();
                String keycloakId = (String) g.get("id");
                Map<String, List<String>> attributes = (Map<String, List<String>>) g.get("attributes");

                // 🔹 Générer code = eg01, eg02 ... si absent
                String code;
                if (attributes != null && attributes.containsKey("code") && !attributes.get("code").isEmpty()) {
                    code = attributes.get("code").get(0);
                } else {
                    code = String.format("eg%02d", counter.getAndIncrement());
                }

                // 🔹 label = nom lisible (ou name fallback)
                String label;
                if (attributes != null && attributes.containsKey("nom") && !attributes.get("nom").isEmpty()) {
                    label = attributes.get("nom").get(0);
                } else if (attributes != null && attributes.containsKey("label") && !attributes.get("label").isEmpty()) {
                    label = attributes.get("label").get(0);
                } else {
                    label = (String) g.get("name");
                }

                map.put("id", keycloakId);
                map.put("keycloakId", keycloakId);
                map.put("code", code);
                map.put("label", label);

                // 🔹 utilisateurs du groupe
                List<Map<String, Object>> users = keycloakService.getUsersOfGroup(keycloakId);
                if (users == null || users.isEmpty()) {
                    users = List.of(Map.of("username", "Aucun utilisateur n'est affecté"));
                }
                map.put("utilisateurs", users);

                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // 🔹 Récupérer un groupe précis par son KeycloakId (UUID)
    @GetMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupeById(@PathVariable String groupId) {
        try {
            Map<String, Object> group = keycloakService.getGroupById(groupId);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Erreur lors de la récupération du groupe",
                    "details", e.getMessage()
            ));
        }
    }

}