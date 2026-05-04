
package com.team.apk.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

@Component
@Lazy
public class CompreFaceClient {

    @Value("${compreface.api-url:}")
    private String apiUrl;

    @Value("${compreface.recognition.api-key:}")
    private String faceRecognitionApiKey;

    @Value("${compreface.verification.api-key:}")
    private String faceVerificationApiKey;

    private final RestTemplate rest = new RestTemplate();

    /*
     * Verification de l'utilisateur
     * la verification de fait avec Photo profile et la photo scanner cote client
     */
    public double verify(Long userId, MultipartFile face1, MultipartFile face2) throws IOException {
        ensureVerificationConfigured();
        String url = apiUrl + "api/v1/verification/verify";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Récupérer l'extension du fichier original
        String originalFilename1 = face1.getOriginalFilename();
        String extension1 = getFileExtension(originalFilename1);

        String originalFilename2 = face2.getOriginalFilename();
        String extension2 = getFileExtension(originalFilename2);

        // Créer des ressources avec des noms de fichiers valides (avec extension)
        ByteArrayResource sourceImage = new ByteArrayResource(face1.getBytes()) {
            @Override
            public String getFilename() {
                // Forcer une extension valide si manquante
                if (extension1 != null && !extension1.isEmpty()) {
                    return "source_image." + extension1;
                }
                return "source_image.jpg"; // Extension par défaut
            }
        };

        ByteArrayResource targetImage = new ByteArrayResource(face2.getBytes()) {
            @Override
            public String getFilename() {
                if (extension2 != null && !extension2.isEmpty()) {
                    return "target_image." + extension2;
                }
                return "target_image.jpg"; // Extension par défaut
            }
        };

        body.add("source_image", sourceImage);
        body.add("target_image", targetImage);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", faceVerificationApiKey);

        ResponseEntity<JsonNode> response = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);

        if (response.getBody().get("result").get(0).get("similarity").asDouble() >= 0.85) {
            saveFace(userId, face1);
        }

        return response.getBody().get("result").get(0).get("similarity").asDouble();
    }

    /*
     * Enregistre un visage dans compreface Avec
     * un id compose de "user_"+"id de l'utilisateur en question"
     */
    public String saveFace(Long userId, MultipartFile photo) throws IOException {
        ensureRecognitionConfigured();

        String url = apiUrl + "/api/v1/recognition/subjects";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource resource = new ByteArrayResource(photo.getBytes()) {
            @Override
            public String getFilename() {
                return photo.getOriginalFilename(); // OBLIGATOIRE
            }
        };

        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", faceRecognitionApiKey);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = rest.exchange(
                url + "?subject=user_" + userId,
                HttpMethod.POST,
                requestEntity,
                JsonNode.class);

        JsonNode bodyResponse = response.getBody();

        if (bodyResponse != null && bodyResponse.has("faceId")) {
            return bodyResponse.get("faceId").asText();
        }

        throw new RuntimeException("Erreur lors de l'enregistrement du visage");
    }

    /*
     * Recherche une Personne dans la base de compreFace
     * avec une photo
     * return --> subject(qui est l'id compose avec "user_"+"l'id de l'utilisateur")
     */

    public String recognizFace(MultipartFile photo) throws IOException {
        ensureRecognitionConfigured();

        String url = apiUrl + "/api/v1/recognition/recognize";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", faceRecognitionApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource resource = new ByteArrayResource(photo.getBytes()) {
            @Override
            public String getFilename() {
                return photo.getOriginalFilename();
            }
        };

        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url + "?limit=1",
                HttpMethod.POST,
                requestEntity,
                JsonNode.class);

        JsonNode result = response.getBody();

        if (result != null && result.has("result") && result.get("result").size() > 0) {
            JsonNode subjects = result.get("result").get(0).get("subjects");
            if (subjects != null && subjects.size() > 0) {
                return subjects.get(0).get("subject").asText();
            }
        }

        return null;
    }

    // Methode pour extraire l'extension dans un fichier
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "jpg"; // Extension par défaut
        }
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) {
            return "jpg"; // Pas d'extension, utiliser par défaut
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    private void ensureVerificationConfigured() {
        if (isBlank(apiUrl) || isBlank(faceVerificationApiKey) || isBlank(faceRecognitionApiKey)) {
            throw new IllegalStateException(
                    "CompreFace n'est pas configuré. Renseigne COMPREFACE_API_URL, COMPREFACE_VERIFICATION_API_KEY et COMPREFACE_RECOGNITION_API_KEY.");
        }
    }

    private void ensureRecognitionConfigured() {
        if (isBlank(apiUrl) || isBlank(faceRecognitionApiKey)) {
            throw new IllegalStateException(
                    "CompreFace n'est pas configuré. Renseigne COMPREFACE_API_URL et COMPREFACE_RECOGNITION_API_KEY.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
