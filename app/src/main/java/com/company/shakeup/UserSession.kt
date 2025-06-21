package com.company.shakeup

class UserSession {
    companion object {
        var UserId: String? = null  // L'ID utilisateur sera stocké ici
        var UserName: String? = null
        var isEmergencyActive = false
    }
}