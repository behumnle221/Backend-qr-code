package com.fapshi.backend.service;

import com.fapshi.backend.dto.response.TransactionDTO;
import com.fapshi.backend.dto.response.TransactionListResponse;
import com.fapshi.backend.dto.response.RetraitResponse;
import com.fapshi.backend.entity.Retrait;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.RetraitRepository;
import com.fapshi.backend.repository.TransactionRepository;
import com.fapshi.backend.repository.VendeurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour les Vendeurs : inscription, recherche, gestion solde.
 */
@Service
public class VendeurService {

    @Autowired
    private VendeurRepository vendeurRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RetraitRepository retraitRepository;

    public Vendeur save(Vendeur vendeur) {
        return vendeurRepository.save(vendeur);
    }

    public Optional<Vendeur> findById(Long id) {
        return vendeurRepository.findById(id);
    }

    public List<Vendeur> findAll() {
        return vendeurRepository.findAll();
    }

    public Optional<Vendeur> findByTelephone(String telephone) {
        return vendeurRepository.findByTelephone(telephone);
    }
    
    public Optional<Vendeur> findByEmail(String email) {
        return vendeurRepository.findByEmail(email);
    }

    /**
     * Calcule le solde virtuel du vendeur (somme des montants nets des transactions SUCCESS)
     * 
     * @param vendeurId ID du vendeur
     * @return BigDecimal solde virtuel
     */
    public BigDecimal calculerSoldeVirtuel(Long vendeurId) {
        // Récupérer toutes les transactions SUCCESS du vendeur via ses QR codes
        List<Transaction> transactions = transactionRepository.findByQrCodeVendeurId(vendeurId);
        
        BigDecimal soldeTotal = BigDecimal.ZERO;
        
        // Sommer uniquement les montantNet des transactions SUCCESS
        for (Transaction tx : transactions) {
            if ("SUCCESS".equals(tx.getStatut()) && tx.getMontantNet() != null) {
                soldeTotal = soldeTotal.add(tx.getMontantNet());
            }
        }
        
        return soldeTotal;
    }

    /**
     * Met à jour le solde virtuel et la date de dernière mise à jour du vendeur
     * Appelé après un paiement SUCCESS ou un retrait
     * 
     * @param vendeurId ID du vendeur
     * @return Vendeur mis à jour
     */
    public Vendeur mettreAJourSolde(Long vendeurId) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé : " + vendeurId));
        
        // Recalculer le solde depuis les transactions
        BigDecimal nouveauSolde = calculerSoldeVirtuel(vendeurId);
        vendeur.setSoldeVirtuel(nouveauSolde);
        vendeur.setDerniereMiseAJourSolde(LocalDateTime.now());
        
        return vendeurRepository.save(vendeur);
    }

    /**
     * Ajoute un montant au solde virtuel (lors d'un paiement SUCCESS)
     * 
     * @param vendeurId ID du vendeur
     * @param montantNet Montant net à ajouter (après commission)
     */
    public void augmenterSolde(Long vendeurId, BigDecimal montantNet) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé : " + vendeurId));
        
        BigDecimal nouveauSolde = vendeur.getSoldeVirtuel().add(montantNet);
        vendeur.setSoldeVirtuel(nouveauSolde);
        vendeur.setDerniereMiseAJourSolde(LocalDateTime.now());
        
        vendeurRepository.save(vendeur);
    }

    /**
     * Réduit le solde virtuel (lors d'un retrait)
     * 
     * @param vendeurId ID du vendeur
     * @param montantRetrait Montant à retirer
     * @throws RuntimeException si solde insuffisant
     */
    public void diminuerSolde(Long vendeurId, BigDecimal montantRetrait) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé : " + vendeurId));
        
        if (vendeur.getSoldeVirtuel().compareTo(montantRetrait) < 0) {
            throw new RuntimeException("Solde insuffisant pour le retrait. Solde: " + vendeur.getSoldeVirtuel() + " XAF, demandé: " + montantRetrait + " XAF");
        }
        
        BigDecimal nouveauSolde = vendeur.getSoldeVirtuel().subtract(montantRetrait);
        vendeur.setSoldeVirtuel(nouveauSolde);
        vendeur.setDerniereMiseAJourSolde(LocalDateTime.now());
        
        vendeurRepository.save(vendeur);
    }

    /**
     * Récupère les transactions d'un vendeur avec pagination et filtres
     * 
     * @param vendeurId ID du vendeur
     * @param statut Filtrer par statut (optionnel)
     * @param dateDebut Filtrer par date de début (optionnel)
     * @param dateFin Filtrer par date de fin (optionnel)
     * @param pageable Pagination (page, size)
     * @return Page de transactions avec métadonnées
     */
    public TransactionListResponse getTransactions(Long vendeurId, String statut, LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findTransactionsByVendeur(vendeurId, statut, dateDebut, dateFin, pageable);
        
        // Convertir Transaction en TransactionDTO avec anonymisation du téléphone
        List<TransactionDTO> dtoList = transactionPage.getContent().stream()
            .map(transaction -> {
                TransactionDTO dto = new TransactionDTO();
                dto.setId(transaction.getId());
                dto.setMontant(transaction.getMontant());
                dto.setCommissionAppliquee(transaction.getCommissionAppliquee() != null ? transaction.getCommissionAppliquee() : BigDecimal.ZERO);
                dto.setMontantNet(transaction.getMontantNet());
                dto.setStatut(transaction.getStatut());
                dto.setDateCreation(transaction.getDateCreation());
                dto.setQrcodeId(transaction.getQrCode() != null ? transaction.getQrCode().getId().toString() : null);
                
                // Anonymiser le téléphone du client (237XXX3456)
                if (transaction.getClient() != null && transaction.getClient().getTelephone() != null) {
                    String phone = transaction.getClient().getTelephone();
                    if (phone.length() >= 4) {
                        dto.setClientTelephone(phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4));
                    } else {
                        dto.setClientTelephone("***");
                    }
                }
                
                // Description du QR code
                if (transaction.getQrCode() != null) {
                    dto.setDescription(transaction.getQrCode().getDescription());
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        // Créer la nouvelle page avec les DTOs
        Page<TransactionDTO> dtoPage = new PageImpl<>(dtoList, pageable, transactionPage.getTotalElements());
        
        return TransactionListResponse.fromPage(dtoPage);
    }

    /**
     * Exporte les transactions d'un vendeur au format CSV
     * 
     * @param vendeurId ID du vendeur
     * @return String contenant le CSV
     */
    public String exportTransactionsToCsv(Long vendeurId) {
        List<Transaction> transactions = transactionRepository.findByQrCodeVendeurId(vendeurId);
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Montant,Commission,Montant Net,Statut,Date,Client,Description\n");
        
        for (Transaction tx : transactions) {
            String phone = "N/A";
            if (tx.getClient() != null && tx.getClient().getTelephone() != null) {
                String fullPhone = tx.getClient().getTelephone();
                if (fullPhone.length() >= 4) {
                    phone = fullPhone.substring(0, 3) + "***" + fullPhone.substring(fullPhone.length() - 4);
                }
            }
            
            String description = tx.getQrCode() != null && tx.getQrCode().getDescription() != null ? 
                tx.getQrCode().getDescription() : "N/A";
            
            csv.append(tx.getId()).append(",")
               .append(tx.getMontant()).append(",")
               .append(tx.getCommissionAppliquee() != null ? tx.getCommissionAppliquee() : "0").append(",")
               .append(tx.getMontantNet()).append(",")
               .append(tx.getStatut()).append(",")
               .append(tx.getDateCreation()).append(",")
               .append(phone).append(",")
               .append(escapeCSV(description))
               .append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Échappe les caractères spéciaux pour CSV
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Crée un retrait si les conditions sont remplies
     * - Solde suffisant
     * - Écart 5h respecté (depuis le dernier retrait SUCCESS/PENDING)
     * 
     * @param vendeurId ID du vendeur
     * @param montant Montant à retirer
     * @param operateur Orange_Cameroon ou MTN_Cameroon
     * @return Retrait créé
     * @throws RuntimeException si conditions non remplies
     */
    public Retrait creerRetrait(Long vendeurId, BigDecimal montant, String operateur) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));
        
        // 1. Vérifier le solde
        if (vendeur.getSoldeVirtuel().compareTo(montant) < 0) {
            throw new RuntimeException("Solde insuffisant. Solde: " + vendeur.getSoldeVirtuel() + " XAF, montant demandé: " + montant + " XAF");
        }
        
        // 2. Vérifier l'écart 5h (depuis le dernier retrait SUCCESS ou PENDING)
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime il5horesAgo = maintenant.minusHours(5);
        
        List<Retrait> recentRetaits = retraitRepository.findRecentRetaits(vendeurId, il5horesAgo);
        if (!recentRetaits.isEmpty()) {
            Retrait dernier = recentRetaits.get(0);
            long heuresEcoulees = java.time.temporal.ChronoUnit.HOURS.between(dernier.getDateCreation(), maintenant);
            throw new RuntimeException("Écart minimum de 5h requir entre les retraits. Dernier retrait: il y a " + heuresEcoulees + "h. Réessayez dans " + (5 - heuresEcoulees) + "h");
        }
        
        // 3. Créer le retrait (état PENDING en attente de confirmation Aangaraa)
        Retrait retrait = new Retrait();
        retrait.setVendeur(vendeur);
        retrait.setMontant(montant);
        retrait.setOperateur(operateur);
        retrait.setStatut("PENDING");
        retrait.setDateCreation(maintenant);
        retrait.setDateAttempt(maintenant);
        
        return retraitRepository.save(retrait);
    }

    /**
     * Récupère l'historique des retraits du vendeur (paginé)
     */
    public Page<RetraitResponse> getRetraits(Long vendeurId, Pageable pageable) {
        Page<Retrait> retraits = retraitRepository.findByVendeurIdOrderByDateCreationDesc(vendeurId, pageable);
        
        return retraits.map(retrait -> new RetraitResponse(
            retrait.getId(),
            retrait.getMontant(),
            retrait.getStatut(),
            retrait.getDateCreation(),
            retrait.getDateAttempt(),
            retrait.getReferenceId(),
            retrait.getOperateur(),
            retrait.getMessage()
        ));
    }

    /**
     * Récupère le dernier retrait du vendeur
     */
    public Optional<RetraitResponse> getDernierRetrait(Long vendeurId) {
        return retraitRepository.findLastRetraitByVendeur(vendeurId)
            .map(retrait -> new RetraitResponse(
                retrait.getId(),
                retrait.getMontant(),
                retrait.getStatut(),
                retrait.getDateCreation(),
                retrait.getDateAttempt(),
                retrait.getReferenceId(),
                retrait.getOperateur(),
                retrait.getMessage()
            ));
    }

    /**
     * Met à jour le statut d'un retrait après réponse Aangaraa
     */
    public Retrait mettreAJourStatutRetrait(Long retraitId, String nouveauStatut, String referenceId, String message) {
        Retrait retrait = retraitRepository.findById(retraitId)
                .orElseThrow(() -> new RuntimeException("Retrait non trouvé"));
        
        retrait.setStatut(nouveauStatut);
        retrait.setReferenceId(referenceId);
        retrait.setMessage(message);
        retrait.setDateAttempt(LocalDateTime.now());
        
        // Si SUCCÈS, déduire du solde du vendeur
        if ("SUCCESS".equals(nouveauStatut)) {
            diminuerSolde(retrait.getVendeur().getId(), retrait.getMontant());
        }
        
        return retraitRepository.save(retrait);
    }
}