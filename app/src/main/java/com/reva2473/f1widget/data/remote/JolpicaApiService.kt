package com.reva2473.f1widget.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ============================================================================
// Jolpica-F1 (Ergast compatible) Retrofit API Interface
// Base URL: https://api.jolpi.ca/ergast/f1/
// ============================================================================

interface JolpicaApiService {

    @GET("seasons.json")
    suspend fun getSeasons(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): JolpicaResponse<SeasonTableWrapper>

    @GET("{season}.json")
    suspend fun getSeasonSchedule(
        @Path("season") season: String,
        @Query("limit") limit: Int = 100
    ): JolpicaResponse<RaceTableWrapper>

    @GET("{season}/driverStandings.json")
    suspend fun getDriverStandings(
        @Path("season") season: String
    ): JolpicaResponse<StandingsTableWrapper>

    @GET("{season}/constructorStandings.json")
    suspend fun getConstructorStandings(
        @Path("season") season: String
    ): JolpicaResponse<StandingsTableWrapper>

    @GET("{season}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("season") season: String,
        @Path("round") round: String
    ): JolpicaResponse<RaceTableWrapper>

    @GET("{season}/{round}/qualifying.json")
    suspend fun getQualifyingResults(
        @Path("season") season: String,
        @Path("round") round: String
    ): JolpicaResponse<RaceTableWrapper>

    @GET("{season}/{round}/pitstops.json")
    suspend fun getPitStops(
        @Path("season") season: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JolpicaResponse<RaceTableWrapper>
}

// ============================================================================
// Jolpica-F1 Response DTOs
// ============================================================================

@Serializable
data class JolpicaResponse<T>(
    @SerialName("MRData") val mrData: MRData<T>
)

@Serializable
data class MRData<T>(
    val xmlns: String? = null,
    val series: String? = null,
    val url: String? = null,
    val limit: String? = null,
    val offset: String? = null,
    val total: String? = null,
    @SerialName("SeasonTable") val seasonTable: T? = null,
    @SerialName("RaceTable") val raceTable: T? = null,
    @SerialName("StandingsTable") val standingsTable: T? = null
)

@Serializable
data class SeasonTableWrapper(
    @SerialName("Seasons") val seasons: List<SeasonDto> = emptyList()
)

@Serializable
data class SeasonDto(
    val season: String,
    val url: String? = null
)

@Serializable
data class RaceTableWrapper(
    val season: String? = null,
    val round: String? = null,
    @SerialName("Races") val races: List<RaceDto> = emptyList()
)

@Serializable
data class RaceDto(
    val season: String,
    val round: String,
    val url: String? = null,
    val raceName: String,
    @SerialName("Circuit") val circuit: CircuitDto,
    val date: String,
    val time: String? = null,
    @SerialName("FirstPractice") val firstPractice: SessionDto? = null,
    @SerialName("SecondPractice") val secondPractice: SessionDto? = null,
    @SerialName("ThirdPractice") val thirdPractice: SessionDto? = null,
    @SerialName("Qualifying") val qualifying: SessionDto? = null,
    @SerialName("Sprint") val sprint: SessionDto? = null,
    @SerialName("Results") val results: List<ResultDto> = emptyList(),
    @SerialName("QualifyingResults") val qualifyingResults: List<QualifyingResultDto> = emptyList(),
    @SerialName("PitStops") val pitStops: List<PitStopDto> = emptyList()
)

@Serializable
data class CircuitDto(
    val circuitId: String,
    val url: String? = null,
    val circuitName: String,
    @SerialName("Location") val location: LocationDto
)

@Serializable
data class LocationDto(
    val lat: String? = null,
    val long: String? = null,
    val locality: String,
    val country: String
)

@Serializable
data class SessionDto(
    val date: String,
    val time: String? = null
)

@Serializable
data class ResultDto(
    val number: String? = null,
    val position: String,
    val positionText: String? = null,
    val points: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructor") val constructor: ConstructorDto,
    val grid: String? = null,
    val laps: String,
    val status: String,
    @SerialName("Time") val time: TimeDto? = null,
    @SerialName("FastestLap") val fastestLap: FastestLapDto? = null
)

@Serializable
data class DriverDto(
    val driverId: String,
    val permanentNumber: String? = null,
    val code: String? = null,
    val url: String? = null,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String? = null,
    val nationality: String
)

@Serializable
data class ConstructorDto(
    val constructorId: String,
    val url: String? = null,
    val name: String,
    val nationality: String? = null
)

@Serializable
data class TimeDto(
    val millis: String? = null,
    val time: String
)

@Serializable
data class FastestLapDto(
    val rank: String? = null,
    val lap: String? = null,
    @SerialName("Time") val time: LapTimeDto? = null,
    @SerialName("AverageSpeed") val averageSpeed: AverageSpeedDto? = null
)

@Serializable
data class LapTimeDto(
    val time: String
)

@Serializable
data class AverageSpeedDto(
    val units: String? = null,
    val speed: String
)

@Serializable
data class QualifyingResultDto(
    val number: String? = null,
    val position: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructor") val constructor: ConstructorDto,
    @SerialName("Q1") val q1: String? = null,
    @SerialName("Q2") val q2: String? = null,
    @SerialName("Q3") val q3: String? = null
)

@Serializable
data class PitStopDto(
    val driverId: String,
    val lap: String,
    val stop: String,
    val time: String,
    val duration: String
)

@Serializable
data class StandingsTableWrapper(
    val season: String? = null,
    val round: String? = null,
    @SerialName("StandingsLists") val standingsLists: List<StandingsListDto> = emptyList()
)

@Serializable
data class StandingsListDto(
    val season: String,
    val round: String? = null,
    @SerialName("DriverStandings") val driverStandings: List<DriverStandingDto> = emptyList(),
    @SerialName("ConstructorStandings") val constructorStandings: List<ConstructorStandingDto> = emptyList()
)

@Serializable
data class DriverStandingDto(
    val position: String,
    val positionText: String? = null,
    val points: String,
    val wins: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructors") val constructors: List<ConstructorDto> = emptyList()
)

@Serializable
data class ConstructorStandingDto(
    val position: String,
    val positionText: String? = null,
    val points: String,
    val wins: String,
    @SerialName("Constructor") val constructor: ConstructorDto
)
