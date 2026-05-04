/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team.apk.Service;

/**
 *
 * @author HP
 */

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

//Service chargé d'envoyer les photos de profil sur Cloudinary.
@Service
public class CloudinaryService {
    // Taille maximale autorisee : 5 Mo.
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryService(Cloudinary cloudinary,
                             @Value("${cloudinary.cloud-name:}") String cloudName,
                             @Value("${cloudinary.api-key:}") String apiKey,
                             @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
//    Envoie une image de profil vers Cloudinary et retourne son URL sécurisée.
    public String uploadProfileImage(MultipartFile photo) throws IOException {
        // Si aucune photo n'est fournie, on ne bloque pas l'inscription.
        if (photo == null || photo.isEmpty()) {
            return null;
        }

        validateConfiguration();
        validateFile(photo);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                photo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "ltc-app/profils",
                        "resource_type", "image",
                        "public_id", "profile_" + UUID.randomUUID(),
                        "overwrite", false
                )
        );

        Object secureUrl = result.get("secure_url");
        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary n'a pas retourné d'URL sécurisée.");
        }

        return secureUrl.toString();
    }

    
//    Vérifie que les clés Cloudinary sont bien renseignées.
    private void validateConfiguration() {
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException(
                    "Configuration Cloudinary incomplète. Renseigne CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY et CLOUDINARY_API_SECRET."
            );
        }
    }
    
//    Vérifie que le fichier envoyé respecte les règles prévues.
    private void validateFile(MultipartFile photo) {
        String contentType = photo.getContentType();
        // On n'accepte que des images.
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier sélectionné doit être une image.");
        }

        if (photo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La photo ne doit pas dépasser 5 Mo.");
        }
    }
    
//    Retourne vrai si la chaîne est nulle, vide ou composée d'espaces.
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
