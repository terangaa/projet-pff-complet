package com.pagam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DemandeProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomProduit;
    private String message;
    private String utilisateurEmail;
    private String status = "EN_ATTENTE"; // EN_ATTENTE, ACCEPTEE, REFUSEE

}
