package com.pagam.repository;

import com.pagam.entity.DemandeProduit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeProduitRepository extends JpaRepository<DemandeProduit, Long> {
    List<DemandeProduit> findByUtilisateurEmail(String email);
}
