package com.team.apk.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "DEMANDE")
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "demande_seq_gen")
    @SequenceGenerator(name = "demande_seq_gen", sequenceName = "SEQ_DEMANDE", allocationSize = 1)
    @Column(name = "DEMANDEID")
    private Long demandeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDDEMANDEUR", nullable = false, foreignKey = @ForeignKey(name = "FK_DEMANDEUR"))
    private Users demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDRECEVEUR", nullable = false, foreignKey = @ForeignKey(name = "FK_RECEVEUR"))
    private Users receveur;

    @Column(name = "STATUT", nullable = false, length = 20)
    private String statut;

    @Column(name = "CREATIONDATE", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "UPDATEDATE")
    private LocalDateTime updateDate;

    public Demande() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (statut == null) {
            statut = "EN ATTENTE";
        }
        if (creationDate == null) {
            creationDate = now;
        }
        if (updateDate == null) {
            updateDate = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = LocalDateTime.now();
    }

    public Long getDemandeId() {
        return demandeId;
    }

    public void setDemandeId(Long demandeId) {
        this.demandeId = demandeId;
    }

    public Users getDemandeur() {
        return demandeur;
    }

    public void setDemandeur(Users demandeur) {
        this.demandeur = demandeur;
    }

    public Users getReceveur() {
        return receveur;
    }

    public void setReceveur(Users receveur) {
        this.receveur = receveur;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }
}