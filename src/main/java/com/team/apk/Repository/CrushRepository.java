package com.team.apk.Repository;

import com.team.apk.Model.Crush;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repository d'accès aux données de l'entité {@link Crush}.
// Cette interface centralise les lectures métier liées aux crushs :
// vérification d'existence, comptage des crushs actifs, récupération
// chronologique et recherche ciblée par propriétaire / cible.
 
public interface CrushRepository extends JpaRepository<Crush, Long> {
    
    //Vérifie si un crush existe déjà entre un propriétaire et une cible donnés.
    @Query("""
           select count(c) > 0
           from Crush c
           where c.owner.userId = :ownerId
             and c.target.userId = :targetId
           """)
    boolean existsByOwnerAndTarget(@Param("ownerId") Long ownerId,
                                   @Param("targetId") Long targetId);

    
    // Compte le nombre de crushs actifs ajoutés par un utilisateur.
    // Seuls les enregistrements dont le statut vaut {@code CRUSH} sont pris en compte.
    @Query("""
       select count(c)
       from Crush c
       where c.owner.userId = :ownerId
         and upper(c.statut) = 'CRUSH'
       """)
    long countActiveAddedByOwnerId(@Param("ownerId") Long ownerId);
    
    // Compte le nombre de crushs actifs reçus par un utilisateur ciblé.
    // Seuls les enregistrements encore actifs sont comptabilisés.
    @Query("""
       select count(c)
       from Crush c
       where c.target.userId = :targetId
         and upper(c.statut) = 'CRUSH'
       """)
    long countActiveReceivedForTargetId(@Param("targetId") Long targetId);
    
    
    //Retourne tous les crushs créés par un utilisateur, du plus récent au plus ancien.
    @Query("""
           select c
           from Crush c
           where c.owner.userId = :ownerId
           order by c.creationDate desc
           """)
    List<Crush> findAddedByOwnerId(@Param("ownerId") Long ownerId);
    
    
    //    Recherche un crush précis appartenant à un utilisateur donné.
    //    Cette méthode est utile pour sécuriser les opérations de mise à jour
    //    en s'assurant que le crush manipulé appartient bien à l'utilisateur courant.
    @Query("""
           select c
           from Crush c
           where c.crushId = :crushId
             and c.owner.userId = :ownerId
           """)
    Optional<Crush> findByCrushIdAndOwnerUserId(@Param("crushId") Long crushId,
                                                @Param("ownerId") Long ownerId);
    
    // Recherche le crush liant exactement un propriétaire à une cible.
    @Query("""
           select c
           from Crush c
           where c.owner.userId = :ownerId
             and c.target.userId = :targetId
           """)
    Optional<Crush> findByOwnerAndTarget(@Param("ownerId") Long ownerId,
                                         @Param("targetId") Long targetId);
    
    
    // Retourne les crushs actifs reçus par un utilisateur, sans exposer l'identité de l'auteur
    @Query("""
           select c
           from Crush c
           where c.target.userId = :targetId
             and upper(c.statut) = 'CRUSH'
           order by c.creationDate desc
           """)
    List<Crush> findActiveReceivedByTargetId(@Param("targetId") Long targetId);
}
