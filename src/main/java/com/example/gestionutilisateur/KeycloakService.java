package com.example.gestionutilisateur;

import com.example.gestionutilisateur.Entities.Groupe;
import com.example.gestionutilisateur.Entities.Utilisateur;
import com.example.gestionutilisateur.Repository.GroupeRepository;
import com.example.gestionutilisateur.Repository.UtilisateurRepository;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeycloakService {

    private Keycloak keycloak;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String targetRealm;

    @Value("${keycloak.admin.realm}")
    private String adminRealm;

    @Value("${keycloak.admin.client-id}")
    private String adminClientId;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    private final UtilisateurRepository utilisateurRepository;
    private final GroupeRepository groupeRepository;

    public KeycloakService(UtilisateurRepository utilisateurRepository, GroupeRepository groupeRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.groupeRepository = groupeRepository;
    }

    @PostConstruct
    public void init() {
        try {
            if (targetRealm == null || targetRealm.isBlank()) {
                throw new IllegalStateException("❌ La propriété 'keycloak.realm' est manquante dans application.properties");
            }

            this.keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(adminRealm)
                    .clientId(adminClientId)
                    .username(adminUsername)
                    .password(adminPassword)
                    .grantType(OAuth2Constants.PASSWORD)
                    .build();

            try {
                syncUsersToLocalDatabase();
            } catch (Exception e) {
                System.out.println("⚠️ Sync Keycloak -> Local DB échouée au démarrage : " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur initialisation Keycloak client : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ==================== GROUPES ==================== */

    public List<GroupRepresentation> getUserGroups(String userId) {
        return Optional.ofNullable(keycloak.realm(targetRealm).users()
                        .get(userId)
                        .groups())
                .orElse(Collections.emptyList());
    }

    /* ==================== UTILISATEURS ==================== */

    public List<UserRepresentation> getAllUsers() {
        List<UserRepresentation> users = Optional.ofNullable(
                keycloak.realm(targetRealm).users().list()
        ).orElse(Collections.emptyList());

        for (UserRepresentation user : users) {
            List<GroupRepresentation> groups = getUserGroups(user.getId());
            if (!groups.isEmpty()) {
                Map<String, List<String>> attrs = user.getAttributes() != null ?
                        new HashMap<>(user.getAttributes()) : new HashMap<>();
                attrs.put("groups", groups.stream()
                        .map(GroupRepresentation::getName)
                        .collect(Collectors.toList()));
                user.setAttributes(attrs);
            }
        }
        return users;
    }

    public void syncUsersToLocalDatabase() {
        List<UserRepresentation> kcUsers = getAllUsers();

        for (UserRepresentation kcUser : kcUsers) {
            try {
                Utilisateur user = utilisateurRepository.findByKeycloakId(kcUser.getId())
                        .orElse(new Utilisateur());

                user.setKeycloakId(kcUser.getId());
                user.setUsername(kcUser.getUsername());
                user.setEmail(kcUser.getEmail());
                user.setFirstName(kcUser.getFirstName());
                user.setLastName(kcUser.getLastName());

                if (kcUser.getAttributes() != null) {
                    user.setPhoneNumber(kcUser.getAttributes()
                            .getOrDefault("phoneNumber", List.of("")).get(0));
                    user.setAddress(kcUser.getAttributes()
                            .getOrDefault("address", List.of("")).get(0));
                }

                List<GroupRepresentation> groups = getUserGroups(kcUser.getId());
                if (!groups.isEmpty()) {
                    GroupRepresentation kcGroup = groups.get(0);
                    String groupName = getAttributeValue(kcGroup, "nom", kcGroup.getName());
                    String groupCode = getAttributeValue(kcGroup, "code", kcGroup.getId());

                    Groupe groupeEntity = groupeRepository.findByLabel(groupName)
                            .orElseGet(() -> {
                                Groupe g = new Groupe();
                                g.setLabel(groupName);
                                g.setCode(groupCode); // ✅ on enregistre le code
                                return groupeRepository.save(g);
                            });

                    // ✅ si le groupe existe déjà mais que le code est vide, on le met à jour
                    if (groupeEntity.getCode() == null || groupeEntity.getCode().isBlank()) {
                        groupeEntity.setCode(groupCode);
                        groupeRepository.save(groupeEntity);
                    }

                    user.setGroupe(groupeEntity);
                } else {
                    user.setGroupe(null);
                }

                utilisateurRepository.save(user);

            } catch (Exception e) {
                System.out.println("❌ Erreur sync user " + kcUser.getUsername() + " : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void updateUserInKeycloak(Utilisateur utilisateur) {
        if (utilisateur.getKeycloakId() == null) {
            throw new RuntimeException("L'utilisateur n'a pas d'ID Keycloak");
        }

        try {
            var userResource = keycloak.realm(targetRealm).users().get(utilisateur.getKeycloakId());
            UserRepresentation kcUser = userResource.toRepresentation();

            kcUser.setUsername(utilisateur.getUsername());
            kcUser.setFirstName(utilisateur.getFirstName());
            kcUser.setLastName(utilisateur.getLastName());
            kcUser.setEmail(utilisateur.getEmail());

            Map<String, List<String>> attributes = kcUser.getAttributes() != null ?
                    new HashMap<>(kcUser.getAttributes()) : new HashMap<>();
            attributes.put("phoneNumber", List.of(utilisateur.getPhoneNumber() != null ? utilisateur.getPhoneNumber() : ""));
            attributes.put("address", List.of(utilisateur.getAddress() != null ? utilisateur.getAddress() : ""));
            kcUser.setAttributes(attributes);

            userResource.update(kcUser);

            if (utilisateur.getGroupe() != null && utilisateur.getGroupe().getLabel() != null) {
                List<GroupRepresentation> currentGroups = userResource.groups();
                for (GroupRepresentation g : currentGroups) {
                    userResource.leaveGroup(g.getId());
                }

                List<GroupRepresentation> allGroups = keycloak.realm(targetRealm).groups().groups();
                Optional<GroupRepresentation> targetGroup = allGroups.stream()
                        .filter(g -> g.getName().equals(utilisateur.getGroupe().getLabel()))
                        .findFirst();

                targetGroup.ifPresent(group -> userResource.joinGroup(group.getId()));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'utilisateur dans Keycloak : " + e.getMessage(), e);
        }
    }

    public String createUserInKeycloak(Utilisateur utilisateur) {
        // 🔹 Validation des champs obligatoires
        if (utilisateur.getUsername() == null || utilisateur.getUsername().isBlank()) {
            throw new RuntimeException("Le nom d'utilisateur (username) est obligatoire.");
        }
        if (utilisateur.getEmail() == null || utilisateur.getEmail().isBlank()) {
            throw new RuntimeException("L'adresse email est obligatoire.");
        }
        if (utilisateur.getFirstName() == null || utilisateur.getFirstName().isBlank()) {
            throw new RuntimeException("Le prénom est obligatoire.");
        }
        if (utilisateur.getLastName() == null || utilisateur.getLastName().isBlank()) {
            throw new RuntimeException("Le nom est obligatoire.");
        }

        // 🔹 Vérification si l'utilisateur existe déjà dans Keycloak
        List<UserRepresentation> existingUsers = keycloak.realm(targetRealm).users().search(utilisateur.getUsername());
        if (!existingUsers.isEmpty()) {
            throw new RuntimeException("Un utilisateur avec ce username existe déjà dans Keycloak.");
        }
        List<UserRepresentation> existingByEmail = keycloak.realm(targetRealm)
                .users().search(null, null, null, utilisateur.getEmail(), 0, 1);
        if (!existingByEmail.isEmpty()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà dans Keycloak.");
        }

        // 🔹 Préparation de l'utilisateur Keycloak
        UserRepresentation user = new UserRepresentation();
        user.setUsername(utilisateur.getUsername());
        user.setEmail(utilisateur.getEmail());
        user.setFirstName(utilisateur.getFirstName());
        user.setLastName(utilisateur.getLastName());
        user.setEnabled(true);

        // 🔹 Ajout des attributs personnalisés
        Map<String, List<String>> attributes = new HashMap<>();
        if (utilisateur.getPhoneNumber() != null) {
            attributes.put("phoneNumber", List.of(utilisateur.getPhoneNumber()));
        }
        if (utilisateur.getAddress() != null) {
            attributes.put("address", List.of(utilisateur.getAddress()));
        }

        // 🔹 Mot de passe conforme (généré si absent)
        String password = utilisateur.getPassword();
        if (password == null || password.isBlank()) {
            password = generateSecurePassword(); // conforme à la politique Keycloak
            utilisateur.setPassword(password);
        }

        // ✅ Ajout de l'attribut "password" (obligatoire dans ton profil Keycloak)
        attributes.put("password", List.of(password));

        // 🔹 Affectation des attributs
        user.setAttributes(attributes);

        // 🔹 Credentials Keycloak
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        // 🔹 Log avant envoi
        System.out.println("📤 Envoi à Keycloak : " + user.getUsername() + " / " + user.getEmail() +
                " realm=" + targetRealm + " | Password généré=" + password);

        // 🔹 Appel Keycloak
        Response response = keycloak.realm(targetRealm).users().create(user);

        try {
            String body = response.readEntity(String.class);
            System.out.println("📥 Réponse Keycloak : " + response.getStatus() + " - " + response.getStatusInfo());
            System.out.println("📥 Corps de réponse Keycloak : " + body);
        } catch (Exception e) {
            System.out.println("⚠ Impossible de lire le corps de la réponse Keycloak");
        }

        if (response.getStatus() == 201) {
            return CreatedResponseUtil.getCreatedId(response);
        } else {
            throw new RuntimeException("Erreur création utilisateur Keycloak : " +
                    response.getStatus() + " - " + response.getStatusInfo());
        }
    }

    /**
     * Génère un mot de passe conforme : 8-12 caractères, majuscule, minuscule, chiffre, spécial.
     */
    private String generateSecurePassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*()-_=+<>?";

        String allChars = upperCase + lowerCase + digits + specialChars;
        Random random = new Random();

        // Au moins 1 de chaque type
        StringBuilder password = new StringBuilder();
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Compléter jusqu'à une longueur aléatoire entre 8 et 12
        int targetLength = 8 + random.nextInt(5); // 8 à 12 inclus
        for (int i = 4; i < targetLength; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Mélanger pour éviter une structure prévisible
        List<Character> pwdChars = password.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(pwdChars);
        StringBuilder finalPassword = new StringBuilder();
        pwdChars.forEach(finalPassword::append);

        return finalPassword.toString();
    }

    /**
     * Affecte un utilisateur à un groupe de type "region"
     * et le retire automatiquement de ses autres groupes "region" existants.
     */
    public void affecterUtilisateurAuGroupeRegion(String keycloakId, String regionLabel) {
        if (keycloakId == null || keycloakId.isEmpty()) {
            throw new IllegalArgumentException("Le keycloakId de l'utilisateur ne peut pas être null ou vide");
        }
        if (regionLabel == null || regionLabel.isEmpty()) {
            throw new IllegalArgumentException("Le label de la région ne peut pas être null ou vide");
        }

        try {
            // 🔹 Récupération de l'utilisateur dans Keycloak
            var userResource = keycloak.realm(targetRealm).users().get(keycloakId);
            if (userResource == null) {
                throw new RuntimeException("Utilisateur Keycloak introuvable pour keycloakId : " + keycloakId);
            }
            System.out.println("[INFO] Utilisateur trouvé dans Keycloak : " + keycloakId);

            // 🔹 Supprimer l'utilisateur des anciens groupes de type "region"
            List<GroupRepresentation> userGroups = userResource.groups();
            for (GroupRepresentation g : userGroups) {
                String type = getAttributeValue(g, "type", "");
                if ("region".equalsIgnoreCase(type)) {
                    System.out.println("[INFO] Suppression de l'utilisateur du groupe existant : " + g.getName());
                    userResource.leaveGroup(g.getId());
                }
            }

            // 🔹 Chercher le groupe cible dans Keycloak par label (nom lisible)
            List<GroupRepresentation> allGroups = keycloak.realm(targetRealm).groups().groups();
            Optional<GroupRepresentation> targetGroupOpt = findGroupByLabel(allGroups, regionLabel);

            if (targetGroupOpt.isEmpty()) {
                throw new RuntimeException("Groupe Keycloak introuvable pour le label : " + regionLabel);
            }

            GroupRepresentation targetGroup = targetGroupOpt.get();
            System.out.println("[INFO] Groupe cible trouvé : " + targetGroup.getName() + " (ID : " + targetGroup.getId() + ")");

            // 🔹 Affecter l'utilisateur au groupe
            userResource.joinGroup(targetGroup.getId());
            System.out.println("[SUCCESS] Utilisateur " + keycloakId + " affecté au groupe " + targetGroup.getName());

        } catch (Exception e) {
            System.err.println("[ERROR] Erreur lors de l'affectation de l'utilisateur au groupe region : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'affectation de l'utilisateur au groupe region : " + e.getMessage(), e);
        }
    }

    private Optional<GroupRepresentation> findGroupByLabel(List<GroupRepresentation> groups, String label) {
        if (groups == null) return Optional.empty();
        for (GroupRepresentation g : groups) {
            if (label.equalsIgnoreCase(g.getName())) {
                return Optional.of(g);
            }
            // Recherche récursive dans les sous-groupes
            Optional<GroupRepresentation> found = findGroupByLabel(g.getSubGroups(), label);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    /* ==================== GROUPES ==================== */

    public List<Groupe> getRootGroupsFromKeycloak() {
        List<GroupRepresentation> kcGroups = Optional.ofNullable(
                keycloak.realm(targetRealm).groups().groups()
        ).orElse(Collections.emptyList());

        return kcGroups.stream()
                .map(g -> {
                    Groupe groupe = new Groupe();
                    groupe.setId(0L); // id interne inutilisé ici
                    groupe.setCode(getAttributeValue(g, "code", g.getId())); // code du groupe
                    groupe.setLabel(getAttributeValue(g, "nom", g.getName())); // nom lisible
                    return groupe;
                })
                .collect(Collectors.toList());
    }

    private String getAttributeValue(GroupRepresentation group, String key, String defaultValue) {
        return Optional.ofNullable(group.getAttributes())
                .map(attrs -> attrs.get(key))
                .filter(values -> !values.isEmpty() && values.get(0) != null && !values.get(0).isBlank())
                .map(values -> values.get(0))
                .orElse(defaultValue);
    }

    public List<Map<String, Object>> getAllGroupsFromKeycloak() {
        List<Map<String, Object>> allGroups = new ArrayList<>();
        List<GroupRepresentation> roots = Optional.ofNullable(keycloak.realm(targetRealm).groups().groups())
                .orElse(Collections.emptyList());
        roots.forEach(root -> collectGroupsRecursive(root, allGroups));
        return allGroups;
    }

    private void collectGroupsRecursive(GroupRepresentation group, List<Map<String, Object>> list) {
        list.add(mapGroupToDTO(group));
        if (group.getSubGroups() != null) {
            group.getSubGroups().forEach(sub -> collectGroupsRecursive(sub, list));
        }
    }

    private Map<String, Object> mapGroupToDTO(GroupRepresentation group) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("code", getAttributeValue(group, "code", ""));
        map.put("type", getAttributeValue(group, "type", ""));
        map.put("nom", getAttributeValue(group, "nom", ""));
        map.put("label", getAttributeValue(group, "nom", ""));
        return map;
    }

    public String createGroup(String name, String type, String code, String nom) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        group.setAttributes(Map.of(
                "type", List.of(type == null ? "" : type),
                "code", List.of(code == null ? "" : code),
                "nom", List.of(nom == null ? "" : nom)
        ));

        Response response = keycloak.realm(targetRealm).groups().add(group);
        if (response.getStatus() == 201) {
            return CreatedResponseUtil.getCreatedId(response);
        }
        throw new RuntimeException("Erreur création groupe : " + response.getStatus());
    }

    public void addChildGroup(String parentGroupId, String childName, String type, String code, String nom) {
        GroupRepresentation child = new GroupRepresentation();
        child.setName(childName);
        child.setAttributes(Map.of(
                "type", List.of(type == null ? "" : type),
                "code", List.of(code == null ? "" : code),
                "nom", List.of(nom == null ? "" : nom)
        ));

        Response response = keycloak.realm(targetRealm).groups()
                .group(parentGroupId)
                .subGroup(child);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Erreur création sous-groupe : " + response.getStatus());
        }
    }

    public void updateGroupAttributes(String groupId, String name, String type, String code, String nom) {
        GroupRepresentation group = keycloak.realm(targetRealm).groups().group(groupId).toRepresentation();
        if (group == null) throw new RuntimeException("Groupe introuvable: " + groupId);

        if (name != null && !name.isBlank()) group.setName(name);

        Map<String, List<String>> attrs = Optional.ofNullable(group.getAttributes()).orElse(new HashMap<>());
        if (type != null && !type.isBlank()) attrs.put("type", List.of(type));
        if (code != null) attrs.put("code", List.of(code));
        if (nom != null) attrs.put("nom", List.of(nom));

        group.setAttributes(attrs);
        keycloak.realm(targetRealm).groups().group(groupId).update(group);
    }

    public List<Map<String, Object>> getGroupHierarchy() {
        List<GroupRepresentation> roots = Optional.ofNullable(
                keycloak.realm(targetRealm).groups().groups()
        ).orElse(Collections.emptyList());

        return roots.stream()
                .map(this::mapGroupWithChildren)
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapGroupWithChildren(GroupRepresentation group) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("code", getAttributeValue(group, "code", ""));
        map.put("label", getAttributeValue(group, "nom", group.getName())); // nom lisible
        map.put("children", group.getSubGroups() == null ? List.of() :
                group.getSubGroups().stream()
                        .map(this::mapGroupWithChildren)
                        .collect(Collectors.toList()));
        return map;
    }

    public List<Map<String, Object>> getUsersOfGroup(String groupId) {
        try {
            List<UserRepresentation> members = keycloak.realm(targetRealm)
                    .groups().group(groupId).members();

            if (members.isEmpty()) {
                Map<String, Object> emptyUser = new HashMap<>();
                emptyUser.put("username", "Aucun utilisateur n'est affecté");
                return Collections.singletonList(emptyUser);
            }

            return members.stream().map(u -> {
                Map<String, Object> uMap = new HashMap<>();
                uMap.put("username", u.getUsername());
                uMap.put("firstName", u.getFirstName());
                uMap.put("lastName", u.getLastName());
                uMap.put("email", u.getEmail());
                return uMap;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            Map<String, Object> emptyUser = new HashMap<>();
            emptyUser.put("username", "Aucun utilisateur n'est affecté");
            return Collections.singletonList(emptyUser);
        }
    }

    public Map<String, Object> getGroupById(String groupId) {
        GroupRepresentation group = keycloak.realm(targetRealm).groups().group(groupId).toRepresentation();
        if (group == null) return null;

        // Récupérer les utilisateurs du groupe
        List<Map<String, Object>> utilisateurs = getUsersOfGroup(group.getId());

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", group.getName());   // ou group.getId() si tu veux l’UUID
        dto.put("keycloakId", group.getId());
        dto.put("code", group.getName());
        dto.put("label", group.getName());
        dto.put("utilisateurs", utilisateurs);

        return dto;
    }
}