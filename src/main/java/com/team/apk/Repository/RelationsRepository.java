package com.team.apk.Repository;

import com.team.apk.Model.Relations;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * Repository d'accès aux données des relations entre utilisateurs.
 *
 * Il regroupe les requêtes métier utilisées pour :
 * - compter les relations actives ou historiques ;
 * - retrouver la relation active d'un utilisateur ;
 * - retrouver une relation entre deux personnes ;
 * - récupérer les anciennes relations.
 */

public interface RelationsRepository extends JpaRepository<Relations, Long> {
    
    
    //Compte le nombre de relations actives d'un utilisateur.
    //Une relation est considérée active lorsque son statut vaut {@code EN COUPLE}.
    @Query("""
           select count(r)
           from Relations r
           where (r.user1.userId = :userId or r.user2.userId = :userId)
             and upper(r.statut) = 'EN COUPLE'
           """)
    long countActiveRelationsForUser(@Param("userId") Long userId);
    
    //Retourne les relations actives d'un utilisateur, triées de la plus récente à la plus ancienne.
    @Query("""
           select r
           from Relations r
           where (r.user1.userId = :userId or r.user2.userId = :userId)
             and upper(r.statut) = 'EN COUPLE'
           order by r.creationDate desc
           """)
    List<Relations> findActiveRelationsForUser(@Param("userId") Long userId);

    
    //Recherche la relation active existant entre deux utilisateurs, indépendamment de leur ordre.
    @Query("""
           select r
           from Relations r
           where ((r.user1.userId = :userA and r.user2.userId = :userB)
               or (r.user1.userId = :userB and r.user2.userId = :userA))
             and upper(r.statut) = 'EN COUPLE'
           """)
    Optional<Relations> findActiveRelationBetweenUsers(@Param("userA") Long userA,
                                                       @Param("userB") Long userB);
    
    
    //Compte toutes les relations d'un utilisateur, quel que soit leur statut.
    @Query("""
           select count(r)
           from Relations r
           where r.user1.userId = :userId
              or r.user2.userId = :userId
           """)
    long countAllRelationsForUser(@Param("userId") Long userId);
    
    //Indique si l'utilisateur possède au moins une relation active.
    @Query("""
           select count(r) > 0
           from Relations r
           where (r.user1.userId = :userId or r.user2.userId = :userId)
             and upper(r.statut) = 'EN COUPLE'
           """)
    boolean existsActiveRelationForUser(@Param("userId") Long userId);
    
    
     //Recherche une relation par identifiant en s'assurant que l'utilisateur donné en fait partie.
    @Query("""
           select r
           from Relations r
           where r.relationId = :relationId
             and (r.user1.userId = :userId or r.user2.userId = :userId)
           """)
    Optional<Relations> findOwnedByRelationIdAndUserId(@Param("relationId") Long relationId,
                                                       @Param("userId") Long userId);
    
    
     //Retourne les anciennes relations d'un utilisateur.
     //Le tri privilégie la date de fin lorsque disponible, sinon la date de création.
    @Query("""
           select r
           from Relations r
           where (r.user1.userId = :userId or r.user2.userId = :userId)
             and upper(r.statut) = 'EX'
           order by coalesce(r.endDate, r.creationDate) desc
           """)
    List<Relations> findExRelationsForUser(@Param("userId") Long userId);

}
