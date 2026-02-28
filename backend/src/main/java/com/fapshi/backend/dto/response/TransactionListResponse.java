package com.fapshi.backend.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO pour la réponse paginée des transactions
 */
public class TransactionListResponse {
    
    private List<TransactionDTO> content;      // Les transactions de cette page
    private long totalElements;                 // Nombre total de transactions
    private int totalPages;                     // Nombre total de pages
    private int currentPage;                    // Page actuelle (0-based)
    private int pageSize;                       // Nombre d'éléments par page
    private boolean hasNextPage;                // Y a-t-il une page suivante ?
    private boolean hasPreviousPage;            // Y a-t-il une page précédente ?
    
    // Constructeurs
    public TransactionListResponse() {}

    public TransactionListResponse(List<TransactionDTO> content, long totalElements, int totalPages, 
                                   int currentPage, int pageSize, boolean hasNextPage, boolean hasPreviousPage) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
    }

    // Getters et Setters
    public List<TransactionDTO> getContent() {
        return content;
    }

    public void setContent(List<TransactionDTO> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }
    
    /**
     * Créer une réponse depuis une Page Spring
     */
    public static TransactionListResponse fromPage(Page<TransactionDTO> page) {
        TransactionListResponse response = new TransactionListResponse();
        response.setContent(page.getContent());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setCurrentPage(page.getNumber());
        response.setPageSize(page.getSize());
        response.setHasNextPage(page.hasNext());
        response.setHasPreviousPage(page.hasPrevious());
        return response;
    }
}
