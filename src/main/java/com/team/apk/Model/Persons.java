package com.team.apk.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "PERSONS")
public class Persons {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "persons_seq_gen")
    @SequenceGenerator(name = "persons_seq_gen", sequenceName = "SEQ_PERSONS", allocationSize = 1)
    @Column(name = "PERSONID")
    private Long personId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "USERID",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "FK_USERID_PERSON")
    )
    private Users user;

    @Column(name = "NOM", length = 50)
    private String nom;

    @Column(name = "PRENOM", length = 50)
    private String prenom;

    @Column(name = "SEXE", length = 1)
    private String sexe;

    @Column(name = "FACEID", unique = true, length = 255)
    private String faceId;

    @Column(name = "PHOTOURL", length = 500)
    private String photoUrl;

    @Column(name = "CREATIONDATE", nullable = false)
    private LocalDateTime creationDate;

    public Persons() {
    }

    @PrePersist
    public void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getFaceId() {
        return faceId;
    }

    public void setFaceId(String faceId) {
        this.faceId = faceId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
