import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call

interface SafetyApiService {
    @POST("/safety-check/")
    fun checkSafety(@Body request: SafetyRequest): Call<SafetyResponse>
}
