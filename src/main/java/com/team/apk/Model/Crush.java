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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "CRUSHES",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_CRUSH_OWNER_TARGET", columnNames = {"OWNERID", "TARGETID"})
        }
)
public class Crush {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crush_seq_gen")
    @SequenceGenerator(name = "crush_seq_gen", sequenceName = "SEQ_CRUSHES", allocationSize = 1)
    @Column(name = "CRUSHID")
    private Long crushId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNERID", nullable = false, foreignKey = @ForeignKey(name = "FK_CRUSH_OWNER"))
    private Users owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TARGETID", nullable = false, foreignKey = @ForeignKey(name = "FK_CRUSH_TARGET"))
    private Users target;

    @Column(name = "STATUT", nullable = false, length = 20)
    private String statut;

    @Column(name = "CREATIONDATE", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "ENDDATE")
    private LocalDateTime endDate;

    public Crush() {
    }

    @PrePersist
    public void prePersist() {
        if (statut == null) {
            statut = "CRUSH";
        }
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public Long getCrushId() {
        return crushId;
    }

    public void setCrushId(Long crushId) {
        this.crushId = crushId;
    }

    public Users getOwner() {
        return owner;
    }

    public void setOwner(Users owner) {
        this.owner = owner;
    }

    public Users getTarget() {
        return target;
    }

    public void setTarget(Users target) {
        this.target = target;
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

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
