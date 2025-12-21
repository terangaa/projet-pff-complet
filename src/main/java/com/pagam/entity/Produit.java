package com.pagam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private Double prix;
    private Integer quantite;
    private int stock;
    private String image;
    private boolean promo;
    private Boolean nouveau;
    private String description;
    private LocalDateTime dateCreation = LocalDateTime.now();

    @ManyToOne
    private Producteur agriculteur;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LignePanier> lignesPanier = new ArrayList<>();

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignesCommande = new ArrayList<>();

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vente> ventes = new ArrayList<>();

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commande> commandes = new ArrayList<>();

    @OneToMany(mappedBy = "produit")
    private List<CommandeValidation> commandeValidations;

    @OneToMany(mappedBy = "produit")
    private List<Commande> commandeAnnulees;


}

