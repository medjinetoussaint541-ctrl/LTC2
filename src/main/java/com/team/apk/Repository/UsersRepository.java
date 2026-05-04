package com.team.apk.Repository;

import com.team.apk.Model.StatutLine;
import com.team.apk.Model.Users;
import com.team.apk.Model.Visibilite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsersRepository extends JpaRepository<Users, Long> {
    
    //Compte le nombre d'utilisateurs possédant exactement l'email fourni.
    @Query(value = "select count(*) from USERS where EMAIL = :email", nativeQuery = true)
    long countByEmailNative(@Param("email") String email);
    
    //Recherche un utilisateur par email.
    Optional<Users> findByEmail(String email);

//    Recherche d'utilisateurs par email exact ou par prénom/nom (insensible à la casse).
//    Exclut l'utilisateur courant des résultats
    @Query("""
           select u
           from Users u
           left join u.person p
           where u.userId <> :excludeUserId
             and u.emailVerified = true
             and (
                 lower(u.email)    like lower(concat('%', :term, '%'))
              or lower(p.prenom)   like lower(concat('%', :term, '%'))
              or lower(p.nom)      like lower(concat('%', :term, '%'))
              or lower(concat(coalesce(p.prenom,''), ' ', coalesce(p.nom,'')))
                                   like lower(concat('%', :term, '%'))
             )
           order by p.prenom asc, p.nom asc
           """)
    List<Users> searchByTermExcluding(@Param("term") String term,
                                      @Param("excludeUserId") Long excludeUserId);

    @Query("""
           select u
           from Users u
           left join u.person p
           where u.userId <> :excludeUserId
             and u.emailVerified = true
             and u.statutLine = :statut
             and u.visibilite = :visibilite
           order by u.lastSeen desc, p.prenom asc, p.nom asc
           """)
    List<Users> findByStatutLineExcluding(@Param("statut") StatutLine statut,
                                          @Param("visibilite") Visibilite visibilite,
                                          @Param("excludeUserId") Long excludeUserId);
}
