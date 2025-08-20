package com.example.gestionutilisateur.Web;

import com.example.gestionutilisateur.Entities.Groupe;
import com.example.gestionutilisateur.KeycloakService;
import com.example.gestionutilisateur.Repository.GroupeRepository;
import com.example.gestionutilisateur.Repository.UtilisateurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

            List<Map<String, Object>> result = keycloakGroups.stream().map(g -> {
                Map<String, Object> map = new HashMap<>();
                String keycloakId = (String) g.get("id");
                Map<String, List<String>> attributes = (Map<String, List<String>>) g.get("attributes");

                // 🔹 Utiliser code et label depuis Keycloak si définis, sinon fallback sur name
                String codeMetier = (attributes != null && attributes.containsKey("code") && !attributes.get("code").isEmpty())
                        ? attributes.get("code").get(0)
                        : (String) g.get("code");

                String label = (attributes != null && attributes.containsKey("label") && !attributes.get("label").isEmpty())
                        ? attributes.get("label").get(0)
                        : (String) g.get("name");

                map.put("id", codeMetier);
                map.put("keycloakId", keycloakId);
                map.put("code", codeMetier);
                map.put("label", label);

                // 🔹 Utilisateurs (inchangé)
                Groupe groupeEntity = groupeRepository.findByCode(codeMetier).orElse(null);
                List<Map<String, Object>> users;
                if (groupeEntity != null) {
                    users = utilisateurRepository.findByGroupe(groupeEntity).stream()
                            .map(u -> {
                                Map<String, Object> uMap = new HashMap<>();
                                uMap.put("id", u.getId());
                                uMap.put("username", u.getUsername());
                                uMap.put("email", u.getEmail());
                                uMap.put("firstName", u.getFirstName());
                                uMap.put("lastName", u.getLastName());
                                return uMap;
                            }).collect(Collectors.toList());
                } else {
                    users = new ArrayList<>();
                }

                if (users.isEmpty()) {
                    Map<String, Object> emptyUser = new HashMap<>();
                    emptyUser.put("username", "Aucun utilisateur n'est affecté");
                    users.add(emptyUser);
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

}