package io.foreversoft.whereuis.model

import java.io.Serializable

data class SecurityInfo (
    val generatedPassword: String,
    val publicKey: String,
    val privateKey: String
) : Serializable