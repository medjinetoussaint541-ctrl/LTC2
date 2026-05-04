package com.team.apk.Repository;

import com.team.apk.Model.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
           select m
           from ChatMessage m
           join fetch m.sender sender
           left join fetch sender.person
           where m.relation.relationId = :relationId
           order by m.creationDate asc, m.messageId asc
           """)
    List<ChatMessage> findConversationMessages(@Param("relationId") Long relationId);

    @Query("""
           select m
           from ChatMessage m
           join fetch m.sender sender
           left join fetch sender.person
           where m.relation.relationId = :relationId
             and m.messageId > :afterId
           order by m.creationDate asc, m.messageId asc
           """)
    List<ChatMessage> findConversationMessagesAfter(@Param("relationId") Long relationId,
                                                    @Param("afterId") Long afterId);

    @Query("""
           select count(m)
           from ChatMessage m
           where m.relation.relationId = :relationId
             and m.sender.userId <> :currentUserId
             and (:lastReadMessageId is null or m.messageId > :lastReadMessageId)
           """)
    long countUnreadMessages(@Param("relationId") Long relationId,
                             @Param("currentUserId") Long currentUserId,
                             @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("""
           select max(m.messageId)
           from ChatMessage m
           where m.relation.relationId = :relationId
             and m.sender.userId <> :currentUserId
             and (:maxMessageId is null or m.messageId <= :maxMessageId)
           """)
    Long findLatestPartnerMessageIdUpTo(@Param("relationId") Long relationId,
                                        @Param("currentUserId") Long currentUserId,
                                        @Param("maxMessageId") Long maxMessageId);
}
