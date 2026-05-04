package com.team.apk.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import com.team.apk.Dto.VerificationResponse;
import com.team.apk.Model.Persons;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;

import jakarta.transaction.Transactional;

@Service
public class VerificationUserService {

    private CompreFaceClient compreFace;
    private UsersRepository userRepo;

    public VerificationUserService(CompreFaceClient compreFace, UsersRepository userRepo) {
        this.compreFace = compreFace;
        this.userRepo = userRepo;
    }


    //Verifier verifier un utilisateur (photo scanner et photo profile)
    @Transactional
    public VerificationResponse verifySelfie(Long userId, MultipartFile selfie) throws Exception {

        Users user = userRepo.findById(userId).orElseThrow();
        Persons persons = user.getPerson();

        if (persons.getPhotoUrl() == null) {
            return null;
        }

        //Telechargement photo profile depuis cloudynary
        byte[] profilPhoto = downloadPhoto(persons.getPhotoUrl());
        MultipartFile profileFile = new MockMultipartFile("profile", profilPhoto);

        System.out.println("=======Vérification du selfie pour l'utilisateur : " + user.getUserId()+"=========");
        double similarity = compreFace.verify(user.getUserId(),profileFile, selfie);
        System.out.println("=======Similarité : " + similarity+"=========");
        boolean success = similarity >= 0.85;

         if (success) {
            user.setUserverified(true);
            user.setUserverifiedat(LocalDateTime.now());
            userRepo.save(user);
            // Enregistrer dans CompreFace
            compreFace.saveFace(userId, selfie);
         }

         return new VerificationResponse(success, similarity, null);

    }

    //Service pour de recherche avec photo
    public Long recognizeUser(MultipartFile photo) throws Exception {
        String subject = compreFace.recognizFace(photo);
        
        if (subject != null && subject.startsWith("user_")) {
            return Long.parseLong(subject.substring(5)); //retourne l'id de l'utilisateur trouver
        }
        
        return null;
    }

    //Enregistre un visage dans compreFace
    public String saveFace(long userId, MultipartFile face) throws Exception{
        return compreFace.saveFace(userId, face);
    }

    //telechharger la photo de profile depuis cloudynary
    public byte[] downloadPhoto(String photoUrl) throws Exception {

       System.out.println("=======Téléchargement de la photo depuis : " + photoUrl+"=========");

        byte[] imageByte = null;

        if (photoUrl == null || photoUrl.isEmpty()) {
            System.err.println("URL de la photo est null ou vide");
            throw new IllegalArgumentException("URL de la photo est null ou vide");
        }

        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(photoUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            // Vérifier le code de réponse
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Erreur lors du téléchargement: HTTP " + responseCode);
                throw new RuntimeException("Erreur lors du téléchargement: HTTP " + responseCode);
            }

            // Lire l'image
            inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            imageByte = outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement de la photo: " + e.getMessage());
            throw new Exception("Impossible de télécharger la photo: " + e.getMessage());
        }

        return imageByte;
    }

}
