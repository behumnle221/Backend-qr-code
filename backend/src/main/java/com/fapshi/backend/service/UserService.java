package com.fapshi.backend.service;

import com.fapshi.backend.entity.ResetToken;
import com.fapshi.backend.entity.User;
import com.fapshi.backend.repository.ResetTokenRepository;
import com.fapshi.backend.repository.UserRepository;
import com.fapshi.backend.utils.PasswordEncoderUtil;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import jakarta.transaction.Transactional;

/**
 * Service pour gérer les opérations sur l'entité User (classe mère).
 * Utilisé pour l'inscription, la connexion, la recherche par email/téléphone.
 */
@Slf4j
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoderUtil passwordEncoder;  // Si pas déjà
    @Autowired
    private UserRepository userRepository;

    // Sauvegarde un utilisateur (inscription)
    public User save(User user) {
        return userRepository.save(user);
    }

    // Recherche par ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Tous les utilisateurs
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Recherche par email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Recherche par téléphone
    public Optional<User> findByTelephone(String telephone) {
        return userRepository.findByTelephone(telephone);
    }

    // Vérifie si email existe déjà
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Vérifie si téléphone existe déjà
    public boolean existsByTelephone(String telephone) {
        return userRepository.existsByTelephone(telephone);
    }
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            log.info("Tentative de reset pour email non existant : {}", email);
            return;
        }

        String code = String.format("%06d", new Random().nextInt(999999));

        ResetToken token = new ResetToken();
        token.setToken(code);
        token.setUserId(user.getId());
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        resetTokenRepository.deleteByUserId(user.getId());
        resetTokenRepository.save(token);

        try {
            emailService.sendResetCodeEmail(email, code, user.getNom());
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation à {} : {}", email, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi du code de réinitialisation");
        }
    }
public void resetPassword(String code, String newPassword) {
    ResetToken token = resetTokenRepository.findByToken(code)
            .orElseThrow(() -> new RuntimeException("Code de réinitialisation invalide ou expiré"));

    if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("Code de réinitialisation expiré");
    }

    User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    user.setPassword(passwordEncoder.encode(newPassword));  // Hache le nouveau password
    userRepository.save(user);

    resetTokenRepository.delete(token);  // Supprime le token utilisé
}
}