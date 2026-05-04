package com.team.apk.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIQUE")
public class Historique {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historique_seq_gen")
    @SequenceGenerator(name = "historique_seq_gen", sequenceName = "SEQ_HISTORIQUE", allocationSize = 1)
    @Column(name = "HISTORIQUEID")
    private Long historiqueId;

    @Column(name = "TYPEACTION", nullable = false, length = 50)
    private String typeAction;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Column(name = "DATEACTION", nullable = false)
    private LocalDateTime dateAction;

    public Historique() {
    }

    @PrePersist
    public void prePersist() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }

    public Long getHistoriqueId() {
        return historiqueId;
    }

    public void setHistoriqueId(Long historiqueId) {
        this.historiqueId = historiqueId;
    }

    public String getTypeAction() {
        return typeAction;
    }

    public void setTypeAction(String typeAction) {
        this.typeAction = typeAction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateAction() {
        return dateAction;
    }

    public void setDateAction(LocalDateTime dateAction) {
        this.dateAction = dateAction;
    }
}
