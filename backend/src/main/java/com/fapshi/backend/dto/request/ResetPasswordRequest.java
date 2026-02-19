package com.fapshi.backend.dto.request;

public class ResetPasswordRequest {

    private String code;
    private String newPassword;

    // GETTERS obligatoires (manuels)
    public String getCode() {
        return code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    // SETTERS (manuels, recommandés pour @RequestBody)
    public void setCode(String code) {
        this.code = code;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}