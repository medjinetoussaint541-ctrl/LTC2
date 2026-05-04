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

import java.time.LocalDateTime;

@Entity
@Table(name = "RELATIONS")
public class Relations {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relations_seq_gen")
    @SequenceGenerator(name = "relations_seq_gen", sequenceName = "SEQ_RELATIONS", allocationSize = 1)
    @Column(name = "RELATIONID")
    private Long relationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER1ID", nullable = false, foreignKey = @ForeignKey(name = "FK_USER1"))
    private Users user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER2ID", nullable = false, foreignKey = @ForeignKey(name = "FK_USER2"))
    private Users user2;

    @Column(name = "STATUT", nullable = false, length = 20)
    private String statut;

    @Column(name = "CREATIONDATE", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "ENDDATE")
    private LocalDateTime endDate;

    @Column(name = "USER1LASTREADMESSAGEID")
    private Long user1LastReadMessageId;

    @Column(name = "USER2LASTREADMESSAGEID")
    private Long user2LastReadMessageId;

    public Relations() {
    }

    @PrePersist
    public void prePersist() {
        if (statut == null) {
            statut = "EN COUPLE";
        }
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }

    public Users getUser1() {
        return user1;
    }

    public void setUser1(Users user1) {
        this.user1 = user1;
    }

    public Users getUser2() {
        return user2;
    }

    public void setUser2(Users user2) {
        this.user2 = user2;
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

    public Long getUser1LastReadMessageId() {
        return user1LastReadMessageId;
    }

    public void setUser1LastReadMessageId(Long user1LastReadMessageId) {
        this.user1LastReadMessageId = user1LastReadMessageId;
    }

    public Long getUser2LastReadMessageId() {
        return user2LastReadMessageId;
    }

    public void setUser2LastReadMessageId(Long user2LastReadMessageId) {
        this.user2LastReadMessageId = user2LastReadMessageId;
    }
}
