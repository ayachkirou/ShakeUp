data class SafetyRequest(
    val latitude: Double,
    val longitude: Double,
    val hour: Int,
    val gender: String,  // "1" for male, "0" for female
    val age: String?
)
data class SafetyResponse(
    val safe: Int
)