package com.fapshi.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class GenerateQrRequest {

    @NotEmpty(message = "Au moins un produit est requis")
    private List<ProductItemRequest> products;

    private String description; // optionnel

    @NotNull(message = "Date d'expiration obligatoire")
    private LocalDateTime dateExpiration;

    // Getters & Setters
    public List<ProductItemRequest> getProducts() { return products; }
    public void setProducts(List<ProductItemRequest> products) { this.products = products; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
}