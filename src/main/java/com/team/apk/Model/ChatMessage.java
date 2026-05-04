package com.team.apk.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_MESSAGES")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_messages_seq_gen")
    @SequenceGenerator(name = "chat_messages_seq_gen", sequenceName = "SEQ_CHAT_MESSAGES", allocationSize = 1)
    @Column(name = "MESSAGEID")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RELATIONID", nullable = false, foreignKey = @ForeignKey(name = "FK_CHAT_MESSAGE_RELATION"))
    private Relations relation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDERID", nullable = false, foreignKey = @ForeignKey(name = "FK_CHAT_MESSAGE_SENDER"))
    private Users sender;

    @Lob
    @Column(name = "CIPHERTEXT", nullable = false)
    private String cipherText;

    @Column(name = "MESSAGEIV", nullable = false, length = 64)
    private String messageIv;

    @Column(name = "CREATIONDATE", nullable = false)
    private LocalDateTime creationDate;

    public ChatMessage() {
    }

    @PrePersist
    public void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Relations getRelation() {
        return relation;
    }

    public void setRelation(Relations relation) {
        this.relation = relation;
    }

    public Users getSender() {
        return sender;
    }

    public void setSender(Users sender) {
        this.sender = sender;
    }

    public String getCipherText() {
        return cipherText;
    }

    public void setCipherText(String cipherText) {
        this.cipherText = cipherText;
    }

    public String getMessageIv() {
        return messageIv;
    }

    public void setMessageIv(String messageIv) {
        this.messageIv = messageIv;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
