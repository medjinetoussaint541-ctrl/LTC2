package com.team.apk.Repository;

import com.team.apk.Model.Demande;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


// Repository dédié aux accès de données de l'entité {@link Demande}.
// Il regroupe les requêtes nécessaires à la gestion des demandes de relation :
// envois, réceptions, demandes en attente, statistiques et contrôles métier.
public interface DemandeRepository extends JpaRepository<Demande, Long> {

    //Retourne les demandes envoyées par un utilisateur, triées de la plus récente à la plus ancienne.
    @Query("""
           select d
           from Demande d
           where d.demandeur.userId = :userId
           order by d.creationDate desc
           """)
    List<Demande> findSentByUserId(@Param("userId") Long userId);
    
    //Retourne les demandes reçues par un utilisateur, triées par date décroissante.
    @Query("""
           select d
           from Demande d
           where d.receveur.userId = :userId
           order by d.creationDate desc
           """)
    List<Demande> findReceivedByUserId(@Param("userId") Long userId);
    
    //Récupère uniquement les demandes reçues encore en attente de traitement.
    @Query("""
           select d
           from Demande d
           where d.receveur.userId = :userId
             and upper(d.statut) = 'EN ATTENTE'
           order by d.creationDate desc
           """)
    List<Demande> findPendingReceivedByUserId(@Param("userId") Long userId);
    
    //Compte le nombre de demandes reçues encore en attente pour un utilisateur.
    @Query("""
           select count(d)
           from Demande d
           where d.receveur.userId = :userId
             and upper(d.statut) = 'EN ATTENTE'
           """)
    long countPendingReceivedByUserId(@Param("userId") Long userId);
    
    
    //Recherche une demande par son identifiant en vérifiant qu'elle appartient bien au receveur indiqué.
    @Query("""
           select d
           from Demande d
           where d.demandeId = :demandeId
             and d.receveur.userId = :userId
           """)
    Optional<Demande> findByDemandeIdAndReceveurUserId(@Param("demandeId") Long demandeId,
                                                       @Param("userId")    Long userId);

    
//    Recherche une demande par son identifiant côté demandeur.
//    Utile notamment pour autoriser l'annulation uniquement par l'émetteur initial.
    @Query("""
           select d
           from Demande d
           where d.demandeId = :demandeId
             and d.demandeur.userId = :userId
           """)
    Optional<Demande> findByDemandeIdAndDemandeurUserId(@Param("demandeId") Long demandeId,
                                                        @Param("userId")    Long userId);
    
    
    //Compte toutes les demandes où l'utilisateur intervient, en tant que demandeur ou receveur.
    @Query("""
           select count(d)
           from Demande d
           where d.demandeur.userId = :userId
              or d.receveur.userId  = :userId
           """)
    long countInteractionsForUser(@Param("userId") Long userId);

    
//    Vérifie si une demande EN ATTENTE existe déjà entre les deux utilisateurs
//    (dans un sens ou dans l'autre).     Cette méthode évite les doublons fonctionnels côté service.
    @Query("""
           select count(d) > 0
           from Demande d
           where upper(d.statut) = 'EN ATTENTE'
             and (
                 (d.demandeur.userId = :userA and d.receveur.userId = :userB)
              or (d.demandeur.userId = :userB and d.receveur.userId = :userA)
             )
           """)
    boolean existsPendingBetween(@Param("userA") Long userA,
                                 @Param("userB") Long userB);
}
