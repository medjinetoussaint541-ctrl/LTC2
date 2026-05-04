package com.team.apk.Model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "USERS")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq_gen")
    @SequenceGenerator(name = "users_seq_gen", sequenceName = "SEQ_USERS", allocationSize = 1)
    @Column(name = "USERID")
    private Long userId;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "EMAILVERIFIED", nullable = false)
    private Boolean emailVerified;

    @Column(name = "PASSWORD", length = 255)
    private String password;

    @Column(name = "AUTHPROVIDER", nullable = false, length = 20)
    private String authProvider;

    @Column(name = "ROLE", nullable = false, length = 20)
    private String role;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'OFFLINE'")
    @Column(name = "STATUTLINE", nullable = false, length = 20)
    private StatutLine statutLine;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ON'")
    @Column(name = "VISIBILITE", nullable = false, length = 10)
    private Visibilite visibilite;

    @Column(name = "LASTSEEN")
    private LocalDateTime lastSeen;

    @Column(name = "CREATIONDATE", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "USERVERIFIED", nullable = false)
    private Boolean userverified = false;

    @Column(name = "USERVERIFIEDAT")
    private LocalDateTime userverifiedat;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Persons person;

    @OneToMany(mappedBy = "demandeur")
    private List<Demande> demandesEnvoyees = new ArrayList<>();

    @OneToMany(mappedBy = "receveur")
    private List<Demande> demandesRecues = new ArrayList<>();

    @OneToMany(mappedBy = "user1")
    private List<Relations> relationsCommeUser1 = new ArrayList<>();

    @OneToMany(mappedBy = "user2")
    private List<Relations> relationsCommeUser2 = new ArrayList<>();

    public Users() {
    }

    @PrePersist
    public void prePersist() {
        if (authProvider == null) {
            authProvider = "MANUAL";
        }
        if (role == null) {
            role = "USER";
        }
        if (statutLine == null) {
            statutLine = StatutLine.OFFLINE;
        }
        if (visibilite == null) {
            visibilite = Visibilite.ON;
        }
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public StatutLine getStatutLine() {
        return statutLine;
    }

    public void setStatutLine(StatutLine statutLine) {
        this.statutLine = statutLine;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Visibilite getVisibilite() {
        return visibilite;
    }

    public void setVisibilite(Visibilite visibilite) {
        this.visibilite = visibilite;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Persons getPerson() {
        return person;
    }

    public void setPerson(Persons person) {
        this.person = person;
    }

    public List<Demande> getDemandesEnvoyees() {
        return demandesEnvoyees;
    }

    public void setDemandesEnvoyees(List<Demande> demandesEnvoyees) {
        this.demandesEnvoyees = demandesEnvoyees;
    }

    public List<Demande> getDemandesRecues() {
        return demandesRecues;
    }

    public void setDemandesRecues(List<Demande> demandesRecues) {
        this.demandesRecues = demandesRecues;
    }

    public List<Relations> getRelationsCommeUser1() {
        return relationsCommeUser1;
    }

    public void setRelationsCommeUser1(List<Relations> relationsCommeUser1) {
        this.relationsCommeUser1 = relationsCommeUser1;
    }

    public List<Relations> getRelationsCommeUser2() {
        return relationsCommeUser2;
    }

    public void setRelationsCommeUser2(List<Relations> relationsCommeUser2) {
        this.relationsCommeUser2 = relationsCommeUser2;
    }

    public void setUserverified(Boolean userverified) {
        this.userverified = userverified;
    }
    public Boolean getUserverified() {
        return userverified;
    }

    public LocalDateTime getUserverifiedat() {
        return userverifiedat;
    }

    public void setUserverifiedat(LocalDateTime userverifiedat) {
        this.userverifiedat = userverifiedat;
    }
}
