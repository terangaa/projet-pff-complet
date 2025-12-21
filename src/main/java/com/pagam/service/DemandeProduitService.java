package com.pagam.service;

import com.pagam.entity.DemandeProduit;
import com.pagam.repository.DemandeProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemandeProduitService {

    private final DemandeProduitRepository repository;
    private final DemandeProduitRepository demandeProduitRepository;

    // Enregistre la demande et renvoie l'objet complet avec ID
    public DemandeProduit enregistrerDemande(DemandeProduit demande) {
        return repository.save(demande);
    }

    public void accepterDemande(Long id) {
        Optional<DemandeProduit> demandeOpt = repository.findById(id);
        demandeOpt.ifPresent(d -> {
            d.setStatus("ACCEPTEE");
            repository.save(d);
        });
    }

    public void refuserDemande(Long id) {
        Optional<DemandeProduit> demandeOpt = repository.findById(id);
        demandeOpt.ifPresent(d -> {
            d.setStatus("REFUSEE");
            repository.save(d);
        });
    }

    public List<DemandeProduit> findAll() {
        return demandeProduitRepository.findAll();
    }

    // Retourne un Optional contenant la demande si elle existe
    public Optional<DemandeProduit> findById(Long id) {
        return repository.findById(id);
    }

    public List<DemandeProduit> findByUtilisateurEmail(String email) {
        return demandeProduitRepository.findByUtilisateurEmail(email);
    }
}
