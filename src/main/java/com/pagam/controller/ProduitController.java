package com.pagam.controller;

import com.pagam.entity.DemandeProduit;
import com.pagam.entity.Producteur;
import com.pagam.entity.Produit;
import com.pagam.entity.Utilisateur;
import com.pagam.repository.ProducteurRepository;
import com.pagam.repository.ProduitRepository;
import com.pagam.repository.UtilisateurRepository;
import com.pagam.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    private final ProduitService produitService;
    private final ProducteurService producteurService;
    private final StockageService stockageService;
    private  final EmailService emailService;
    private final DemandeProduitService demandeProduitService;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProducteurRepository producteurRepository;
    private final UtilisateurService utilisateurService;

    public ProduitController(ProduitService produitService,
                             ProducteurService producteurService,
                             StockageService stockageService, EmailService emailService, DemandeProduitService demandeProduitService,
                             ProduitRepository produitRepository, UtilisateurRepository utilisateurRepository,
                             ProducteurRepository producteurRepository,
                             UtilisateurService utilisateurService) {
        this.produitService = produitService;
        this.producteurService = producteurService;
        this.stockageService = stockageService;
        this.emailService = emailService;
        this.demandeProduitService = demandeProduitService;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.producteurRepository = producteurRepository;
        this.utilisateurService = utilisateurService;
    }

    // ✅ Liste des produits
    @GetMapping
    public String listeProduits(Model model,
                                Authentication authentication,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "6") int size,
                                @RequestParam(value = "q", defaultValue = "") String query) { // <-- defaultValue = ""

        Pageable pageable = PageRequest.of(page, size);
        Page<Produit> produitPage;

        // 🔍 Si une recherche est saisie, on filtre
        if (!query.trim().isEmpty()) {
            produitPage = produitRepository.findByNomContainingIgnoreCase(query.trim(), pageable);
        } else {
            produitPage = produitRepository.findAll(pageable);
        }

        model.addAttribute("produits", produitPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", produitPage.getTotalPages());
        model.addAttribute("query", query); // toujours défini, même vide

        // Rôle de l'utilisateur connecté
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("VISITEUR");
        model.addAttribute("role", role);

        return "produits/produit";
    }

    // ✅ Redirection automatique selon le rôle
    @GetMapping("/creer")
    public String creerProduitRedirect(Model model, Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/produits/creer-admin";
        } else if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR") || a.getAuthority().equals("ROLE_ACHETEUR"))) {
            return "redirect:/produits/creer-utilisateur";
        } else {
            return "redirect:/produits";
        }
    }

    // ✅ Formulaire création (ADMIN)
    @GetMapping("/creer-admin")
    @Secured("ROLE_ADMIN")
    public String creerProduitFormAdmin(Model model) {
        model.addAttribute("produit", new Produit());
        model.addAttribute("agriculteurs", producteurRepository.findAll());
        return "produits/creer-produit-admin";
    }

    // ✅ Enregistrement nouveau produit (ADMIN)
    @PostMapping("/creer-admin")
    @Secured("ROLE_ADMIN")
    public String creerProduitAdmin(@ModelAttribute Produit produit,
                                    @RequestParam(value = "agriculteurId", required = false) Long agriculteurId,
                                    @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (agriculteurId != null) {
                Producteur agriculteur = producteurRepository.findById(agriculteurId)
                        .orElseThrow(() -> new IllegalArgumentException("L’agriculteur spécifié n’existe pas !"));
                produit.setAgriculteur(agriculteur);
            }

            if (produit.getPrix() == null) produit.setPrix(0.0);
            produitService.ajouterProduit(produit, imageFile);

            redirectAttributes.addFlashAttribute("success", "Produit créé avec succès !");
            return "redirect:/produits";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("produit", produit);
            return "redirect:/produits/creer-admin";
        }
    }

    // ✅ Formulaire création (UTILISATEUR)
    @GetMapping("/creer-utilisateur")
    @Secured("ROLE_AGRICULTEUR")
    public String creerProduitUtilisateurForm(Model model, Principal principal) {

        List<Produit> produitsDisponibles = produitRepository.findByAgriculteurIsNotNull();
        model.addAttribute("produitsDisponibles", produitsDisponibles);

        Producteur currentUserProducteur = producteurService.findByUtilisateurEmail(principal.getName());

        if (currentUserProducteur == null) {
            // Message d'erreur plus informatif
            model.addAttribute("error",
                    "Vous devez compléter votre profil producteur avant de créer des produits. " +
                            "Veuillez contacter l'administrateur ou mettre à jour votre profil.");

            // Retourne la vue avec le message d'erreur
            model.addAttribute("produit", new Produit());
            return "produits/creer-produit-utilisateur";
        }

        model.addAttribute("currentUserProducteurId", currentUserProducteur.getId());
        model.addAttribute("produit", new Produit());

        return "produits/creer-produit-utilisateur";
    }


    // ✅ Création produit (UTILISATEUR)
    @PostMapping("/creer-utilisateur")
    @Secured("ROLE_AGRICULTEUR")
    public String creerProduitUtilisateur(@RequestParam("produitId") Long produitId,
                                          @RequestParam("quantite") int quantite,
                                          Principal principal,
                                          Model model) {

        // Récupération du producteur connecté
        Producteur producteur = producteurService.findByUtilisateurEmail(principal.getName());
        if (producteur == null) {
            model.addAttribute("error", "Vous n'êtes pas un producteur !");
            return "produits/creer-produit-utilisateur";
        }

        // Produit source sélectionné
        Produit produitSource = produitRepository.findById(produitId)
                .orElse(null);
        if (produitSource == null) {
            model.addAttribute("error", "Produit introuvable !");
            return "produits/creer-produit-utilisateur";
        }

        // Vérifie si ce producteur a déjà ce produit
        Optional<Produit> produitExistant = produitRepository.findByNomAndAgriculteur(produitSource.getNom(), producteur);

        if (produitExistant.isPresent()) {
            Produit p = produitExistant.get();
            p.setStock(p.getStock() + quantite);
            produitRepository.save(p);
        } else {
            Produit nouveauProduit = new Produit();
            nouveauProduit.setNom(produitSource.getNom());
            nouveauProduit.setDescription(produitSource.getDescription());
            nouveauProduit.setPrix(produitSource.getPrix());
            nouveauProduit.setStock(quantite);
            nouveauProduit.setAgriculteur(producteur);
            produitRepository.save(nouveauProduit);
        }

        model.addAttribute("success", "Stock mis à jour avec succès !");
        return "redirect:/produits/creer-utilisateur";
    }


    // ✅ Modification (ADMIN)
    @GetMapping("/modifier/{id}")
    @Secured("ROLE_ADMIN")
    public String modifierProduitForm(@PathVariable Long id, Model model, Principal principal) {
        Produit produit = produitService.findById(id);
        if (produit == null) return "redirect:/produits";

        model.addAttribute("agriculteurs", producteurService.findAll());
        model.addAttribute("produit", produit);
        model.addAttribute("isAdmin", utilisateurService.isAdmin(principal.getName()));
        return "produits/modifier-produit";
    }

    @PostMapping("/modifier/{id}")
    @Secured("ROLE_ADMIN")
    public String modifierProduit(@PathVariable Long id,
                                  @ModelAttribute("produit") Produit produitForm,
                                  @RequestParam(required = false) Long agriculteurId,
                                  @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                  RedirectAttributes redirectAttributes) {

        Produit produit = produitService.findById(id);
        if (produit == null) {
            redirectAttributes.addFlashAttribute("error", "Produit introuvable !");
            return "redirect:/produits";
        }

        try {
            produit.setNom(produitForm.getNom());
            produit.setDescription(produitForm.getDescription());
            produit.setPrix(produitForm.getPrix());
            produit.setQuantite(produitForm.getQuantite());

            if (agriculteurId != null) {
                producteurService.findById(agriculteurId).ifPresent(produit::setAgriculteur);
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                // Supprime l'ancienne image dans le dossier "produits"
                stockageService.delete(produit.getImage(), "produits");

                // Sauvegarde la nouvelle image dans le dossier "produits"
                String newImagePath = stockageService.save(imageFile, "produits");

                produit.setImage(newImagePath);
            }

            produitService.save(produit);
            redirectAttributes.addFlashAttribute("success", "Produit modifié avec succès !");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
            e.printStackTrace();
        }

        // 🔹 Redirection correcte vers la page de modification
        return "redirect:/produits/modifier/" + id;
    }

    // ✅ Suppression (ADMIN)
    @GetMapping("/supprimer/{id}")
    public String supprimerProduit(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            produitService.deleteProduit(id);
            redirectAttributes.addFlashAttribute("success", "Produit supprimé avec succès !");
        } catch (DataIntegrityViolationException e) {
            // Capture l'erreur de contrainte de clé étrangère
            redirectAttributes.addFlashAttribute("error",
                    "Impossible de supprimer ce produit : il est déjà lié à des ventes.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors de la suppression du produit.");
        }
        return "redirect:/produits?page=1&size=6";
    }

    // ✅ Détails produit
    @GetMapping("/detail/{id}")
    public String detailProduit(@PathVariable Long id, Model model) {
        Produit produit = produitService.findById(id);
        if (produit == null) return "redirect:/produits";

        model.addAttribute("produit", produit);
        if (produit.getAgriculteur() != null) {
            model.addAttribute("prenomProducteur", produit.getAgriculteur().getPrenom());
            model.addAttribute("nomProducteur", produit.getAgriculteur().getNom());
            model.addAttribute("emailProducteur", produit.getAgriculteur().getEmail());
        }
        return "produits/produit-detail";
    }

    @GetMapping("/demandes")
    @Secured({"ROLE_AGRICULTEUR", "ROLE_ADMIN"})
    public String afficherDemandes(Model model, Principal principal) {
        // Récupère l'utilisateur connecté
        String email = principal.getName();

        // Récupère les demandes de cet utilisateur
        List<DemandeProduit> demandes = demandeProduitService.findByUtilisateurEmail(email);

        model.addAttribute("demandes", demandes);
        return "produits/liste-demandes";
    }

    @PostMapping("/demande-ajout")
    public String demandeAjoutProduit(@RequestParam("nomProduit") String nomProduit,
                                      @RequestParam(value = "message", required = false) String message,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {

        try {
            // 1. Création de la demande
            DemandeProduit demande = new DemandeProduit();
            demande.setNomProduit(nomProduit);
            demande.setMessage(message);

            // 2. Sauvegarde de l'email utilisateur si le champ existe
            try {
                demande.setUtilisateurEmail(principal.getName());
            } catch (Exception e) {
                // Si le champ n'existe pas, on continue sans
            }

            // 3. Enregistrement
            DemandeProduit nouvelleDemande = demandeProduitService.enregistrerDemande(demande);

            // 4. Envoi de l'email à l'admin
            emailService.envoyerMailAdmin(
                    nouvelleDemande.getId(),
                    nouvelleDemande.getNomProduit(),
                    nouvelleDemande.getMessage()
            );

            // ✅ MESSAGE DE SUCCÈS QUI S'AFFICHERA
            redirectAttributes.addFlashAttribute("success",
                    "✅ Demande envoyée avec succès !\n" +
                            "Produit : " + nomProduit + "\n" +
                            "L'administrateur a été notifié et vous répondra sous 48h.");

        } catch (Exception e) {
            // ✅ MESSAGE D'ERREUR EN CAS DE PROBLÈME
            redirectAttributes.addFlashAttribute("error",
                    "❌ Erreur lors de l'envoi de la demande : " + e.getMessage());
        }

        // 5. Redirection vers la liste des produits
        return "redirect:/produits";
    }

    @GetMapping("/accepter/{id}")
    public String accepterDemande(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        demandeProduitService.accepterDemande(id); // logique pour accepter la demande
        redirectAttributes.addFlashAttribute("success", "La demande a été acceptée !");
        return "redirect:/demandes/demandes"; // page admin où on liste les demandes
    }

    @GetMapping("/refuser/{id}")
    public String refuserDemande(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        demandeProduitService.refuserDemande(id); // logique pour refuser la demande
        redirectAttributes.addFlashAttribute("info", "La demande a été refusée.");
        return "redirect:/demandes/demandes";
    }
}