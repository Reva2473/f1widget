package com.reva2473.f1widget.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ============================================================================
// OpenF1 Retrofit API Interface
// Base URL: https://api.openf1.org/v1/
// ============================================================================

interface OpenF1ApiService {

    @GET("sessions")
    suspend fun getSessions(
        @Query("session_key") sessionKey: String? = null,
        @Query("session_name") sessionName: String? = null,
        @Query("session_type") sessionType: String? = null,
        @Query("year") year: Int? = null,
        @Query("country_name") countryName: String? = null,
        @Query("circuit_short_name") circuitShortName: String? = null
    ): List<OpenF1SessionDto>

    @GET("position")
    suspend fun getPositions(
        @Query("session_key") sessionKey: String,
        @Query("driver_number") driverNumber: Int? = null
    ): List<OpenF1PositionDto>

    @GET("intervals")
    suspend fun getIntervals(
        @Query("session_key") sessionKey: String,
        @Query("driver_number") driverNumber: Int? = null
    ): List<OpenF1IntervalDto>

    @GET("laps")
    suspend fun getLaps(
        @Query("session_key") sessionKey: String,
        @Query("driver_number") driverNumber: Int? = null,
        @Query("lap_number") lapNumber: Int? = null
    ): List<OpenF1LapDto>

    @GET("stints")
    suspend fun getStints(
        @Query("session_key") sessionKey: String,
        @Query("driver_number") driverNumber: Int? = null
    ): List<OpenF1StintDto>

    @GET("team_radio")
    suspend fun getTeamRadio(
        @Query("session_key") sessionKey: String,
        @Query("driver_number") driverNumber: Int? = null
    ): List<OpenF1TeamRadioDto>

    @GET("race_control")
    suspend fun getRaceControl(
        @Query("session_key") sessionKey: String,
        @Query("category") category: String? = null,
        @Query("flag") flag: String? = null
    ): List<OpenF1RaceControlDto>

    @GET("overtakes")
    suspend fun getOvertakes(
        @Query("session_key") sessionKey: String
    ): List<OpenF1OvertakeDto>

    @GET("weather")
    suspend fun getWeather(
        @Query("session_key") sessionKey: String
    ): List<OpenF1WeatherDto>

    companion object {
        fun create(
            client: OkHttpClient = OkHttpClient.Builder().build(),
            baseUrl: String = "https://api.openf1.org/v1/"
        ): OpenF1ApiService {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(OpenF1ApiService::class.java)
        }
    }
}

// ============================================================================
// OpenF1 Response DTOs
// ============================================================================

@Serializable
data class OpenF1SessionDto(
    @SerialName("session_key") val sessionKey: Int = 0,
    @SerialName("session_name") val sessionName: String = "",
    @SerialName("session_type") val sessionType: String? = null,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("date_end") val dateEnd: String? = null,
    val year: Int? = null,
    @SerialName("circuit_key") val circuitKey: Int? = null,
    @SerialName("circuit_short_name") val circuitShortName: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("country_key") val countryKey: Int? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("gmt_offset") val gmtOffset: String? = null
)

@Serializable
data class OpenF1PositionDto(
    val date: String? = null,
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("meeting_key") val meetingKey: Int? = null,
    val position: Int = 0,
    @SerialName("session_key") val sessionKey: Int = 0
)

@Serializable
data class OpenF1IntervalDto(
    val date: String? = null,
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("gap_to_leader") val gapToLeader: JsonElement? = null,
    val interval: JsonElement? = null,
    @SerialName("session_key") val sessionKey: Int = 0
) {
    val gapToLeaderDouble: Double?
        get() = runCatching { gapToLeader?.toString()?.trim('"')?.toDoubleOrNull() }.getOrNull()

    val intervalDouble: Double?
        get() = runCatching { interval?.toString()?.trim('"')?.toDoubleOrNull() }.getOrNull()
}

@Serializable
data class OpenF1LapDto(
    val date_start: String? = null,
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("duration_sector_1") val durationSector1: Double? = null,
    @SerialName("duration_sector_2") val durationSector2: Double? = null,
    @SerialName("duration_sector_3") val durationSector3: Double? = null,
    @SerialName("i1_speed") val i1Speed: Int? = null,
    @SerialName("i2_speed") val i2Speed: Int? = null,
    @SerialName("is_pit_out_lap") val isPitOutLap: Boolean? = null,
    @SerialName("lap_duration") val lapDuration: Double? = null,
    @SerialName("lap_number") val lapNumber: Int = 0,
    @SerialName("session_key") val sessionKey: Int = 0,
    @SerialName("st_speed") val stSpeed: Int? = null
)

@Serializable
data class OpenF1StintDto(
    @SerialName("compound") val compound: String = "MEDIUM",
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("lap_end") val lapEnd: Int? = null,
    @SerialName("lap_start") val lapStart: Int = 0,
    @SerialName("session_key") val sessionKey: Int = 0,
    @SerialName("stint_number") val stintNumber: Int = 1,
    @SerialName("tyre_age_at_start") val tyreAgeAtStart: Int? = null
)

@Serializable
data class OpenF1TeamRadioDto(
    val date: String = "",
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("recording_url") val recordingUrl: String? = null,
    @SerialName("session_key") val sessionKey: Int = 0,
    val transcript: String? = null
)

@Serializable
data class OpenF1RaceControlDto(
    val category: String? = null,
    val date: String = "",
    @SerialName("driver_number") val driverNumber: Int? = null,
    val flag: String? = null,
    val lap_number: Int? = null,
    val message: String = "",
    val scope: String? = null,
    val sector: Int? = null,
    @SerialName("session_key") val sessionKey: Int = 0
)

@Serializable
data class OpenF1OvertakeDto(
    val date: String? = null,
    @SerialName("driver_number") val driverNumber: Int? = null,
    @SerialName("overtaken_driver_number") val overtakenDriverNumber: Int? = null,
    val lap_number: Int? = null,
    val position: Int? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    val corner: String? = null
)

@Serializable
data class OpenF1WeatherDto(
    @SerialName("air_temperature") val airTemperature: Double = 0.0,
    val date: String = "",
    val humidity: Double = 0.0,
    val pressure: Double = 0.0,
    val rainfall: Double = 0.0,
    @SerialName("session_key") val sessionKey: Int = 0,
    @SerialName("track_temperature") val trackTemperature: Double = 0.0,
    @SerialName("wind_direction") val windDirection: Double = 0.0,
    @SerialName("wind_speed") val windSpeed: Double = 0.0
)
