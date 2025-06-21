package com.company.shakeup

import com.google.firebase.Timestamp

data class Danger(
    val date_danger: Timestamp? = null, // Timestamp pour la date
    val localisation: String = "",
    val id_user: String = ""
)
