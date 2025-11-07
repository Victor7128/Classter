package com.evalua.classter.network

import com.evalua.classter.models.Area
import com.evalua.classter.models.Capacidad
import com.evalua.classter.models.CapacidadDetallada
import com.evalua.classter.models.CompetenciaTemplate
import com.evalua.classter.models.CompetenciaDetallada
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CurriculumApiService {

    @GET("areas")
    suspend fun getAreas(): List<Area>

    // ✅ Obtener área completa (con competencias anidadas)
    @GET("areas/{id}")
    suspend fun getArea(@Path("id") areaId: Int): Area

    // ✅ Obtener solo las competencias de un área (sin area_id ni area_nombre)
    @GET("areas/{id}/competencias")
    suspend fun getCompetenciasByArea(@Path("id") areaId: Int): List<CompetenciaDetallada>

    // ✅ Obtener todas las competencias (con area_id y area_nombre)
    @GET("competencias")
    suspend fun getAllCompetencias(): List<CompetenciaTemplate>

    @GET("competencias/{id}")
    suspend fun getCompetencia(@Path("id") competenciaId: Int): CompetenciaDetallada

    @GET("capacidades")
    suspend fun getCapacidadesByCompetencia(@Query("competencia_id") competenciaId: Int): List<CapacidadDetallada>

    @GET("capacidades/{id}")
    suspend fun getCapacidad(@Path("id") capacidadId: Int): CapacidadDetallada
}