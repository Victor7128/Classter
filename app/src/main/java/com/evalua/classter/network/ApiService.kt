package com.evalua.classter.network

import com.evalua.classter.models.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== BIMESTERS ==========
    @GET("bimesters/full")
    fun getBimesters(): Call<List<BimesterData>>

    // ========== GRADES ==========
    @POST("bimesters/{bimester_id}/grades")
    fun createGrade(
        @Path("bimester_id") bimesterId: Int,
        @Body body: CreateGradeRequest
    ): Call<Grade>

    @DELETE("grades/{grade_id}")
    fun deleteGrade(@Path("grade_id") gradeId: Int): Call<Void>

    @GET("bimesters/{bimester_id}/grades")
    fun listGrades(@Path("bimester_id") bimesterId: Int): Call<List<Grade>>

    @GET("grades/{grade_id}")
    fun getGrade(@Path("grade_id") gradeId: Int): Call<Grade>

    // ========== SECTIONS ==========
    @POST("grades/{grade_id}/sections")
    fun createSection(
        @Path("grade_id") gradeId: Int,
        @Body body: CreateSectionRequest
    ): Call<Section>

    @DELETE("sections/{section_id}")
    fun deleteSection(@Path("section_id") sectionId: Int): Call<Void>

    @GET("grades/{grade_id}/sections")
    fun listSections(@Path("grade_id") gradeId: Int): Call<List<Section>>

    @GET("sections/{section_id}")
    fun getSection(@Path("section_id") sectionId: Int): Call<Section>

    // ========== STUDENTS ==========
    @POST("sections/{section_id}/students")
    fun createStudent(
        @Path("section_id") sectionId: Int,
        @Body body: CreateStudentRequest
    ): Call<Student>

    // ✅ UNIFICADO: Un solo endpoint para estudiantes
    @GET("sections/{section_id}/students")
    suspend fun getStudents(@Path("section_id") sectionId: Int): List<Student>

    @PUT("students/{student_id}")
    fun updateStudent(
        @Path("student_id") studentId: Int,
        @Body body: CreateStudentRequest
    ): Call<Student>

    @DELETE("students/{student_id}")
    fun deleteStudent(@Path("student_id") studentId: Int): Call<Void>

    // ========== IMPORT STUDENTS ==========
    @POST("sections/{section_id}/students/import")
    fun importStudentsJson(
        @Path("section_id") sectionId: Int,
        @Body body: BatchStudentsRequest
    ): Call<ImportStudentsResponse>

    @Multipart
    @POST("sections/{section_id}/students/import_csv")
    fun importStudentsCsv(
        @Path("section_id") sectionId: Int,
        @Part file: MultipartBody.Part
    ): Call<ImportStudentsResponse>

    @Multipart
    @POST("sections/{section_id}/students/import_txt")
    fun importStudentsTxt(
        @Path("section_id") sectionId: Int,
        @Part file: MultipartBody.Part
    ): Call<ImportStudentsResponse>

    // ========== SESSIONS ==========
    @GET("sections/{section_id}/sessions")
    suspend fun getSessions(@Path("section_id") sectionId: Int): List<Session>

    @POST("sections/{section_id}/sessions")
    suspend fun createSession(
        @Path("section_id") sectionId: Int,
        @Body request: NewSessionRequest
    ): Session

    @GET("sessions/{session_id}")
    suspend fun getSession(@Path("session_id") sessionId: Int): Session

    @PUT("sessions/{session_id}")
    suspend fun updateSession(
        @Path("session_id") sessionId: Int,
        @Body request: UpdateSessionRequest
    ): Session

    @DELETE("sessions/{session_id}")
    suspend fun deleteSession(@Path("session_id") sessionId: Int): Response<Unit>

    // ========== PRODUCTS ==========
    @GET("sessions/{session_id}/products")
    suspend fun getProducts(@Path("session_id") sessionId: Int): List<Product>

    @POST("sessions/{session_id}/products")
    suspend fun createProduct(
        @Path("session_id") sessionId: Int,
        @Body request: Map<String, String>
    ): Product

    @PUT("products/{product_id}")
    suspend fun updateProduct(
        @Path("product_id") productId: Int,
        @Body request: UpdateProductRequest
    ): Product

    @DELETE("products/{product_id}")
    suspend fun deleteProduct(@Path("product_id") productId: Int): Response<Unit>

    // ========== COMPETENCIES ==========
    @GET("sessions/{session_id}/competencies")
    suspend fun getCompetencies(@Path("session_id") sessionId: Int): List<Competency>

    @POST("sessions/{session_id}/competencies")
    suspend fun createCompetency(
        @Path("session_id") sessionId: Int,
        @Body request: NewCompetencyRequest
    ): Competency

    @PUT("competencies/{competency_id}")
    suspend fun updateCompetency(
        @Path("competency_id") competencyId: Int,
        @Body request: UpdateCompetencyRequest
    ): Competency

    @DELETE("competencies/{competency_id}")
    suspend fun deleteCompetency(@Path("competency_id") competencyId: Int): Response<Unit>

    // ========== ABILITIES ==========
    @GET("competencies/{competency_id}/abilities")
    suspend fun getAbilities(@Path("competency_id") competencyId: Int): List<Ability>

    @GET("abilities/{ability_id}")
    suspend fun getAbility(@Path("ability_id") abilityId: Int): Ability

    @POST("competencies/{competency_id}/abilities")
    suspend fun createAbility(
        @Path("competency_id") competencyId: Int,
        @Body request: Map<String, String>
    ): Ability

    @PUT("abilities/{ability_id}")
    suspend fun updateAbility(
        @Path("ability_id") abilityId: Int,
        @Body request: UpdateAbilityRequest
    ): Ability

    @DELETE("abilities/{ability_id}")
    suspend fun deleteAbility(@Path("ability_id") abilityId: Int): Response<Unit>

    // ========== CRITERIA ==========
    @GET("abilities/{ability_id}/criteria")
    suspend fun getCriteria(@Path("ability_id") abilityId: Int): List<Criterion>

    @POST("abilities/{ability_id}/criteria")
    suspend fun createCriterion(
        @Path("ability_id") abilityId: Int,
        @Body request: Map<String, String>
    ): Criterion

    @PUT("criteria/{criterion_id}")
    suspend fun updateCriterion(
        @Path("criterion_id") criterionId: Int,
        @Body request: UpdateCriterionRequest
    ): Criterion

    @DELETE("criteria/{criterion_id}")
    suspend fun deleteCriterion(@Path("criterion_id") criterionId: Int): Response<Unit>

    // ========== EVALUATION CONTEXT ==========
    /**
     * ✅ Contexto para evaluación individual
     * Usado en EvaluationActivity
     */
    @GET("evaluation/context")
    suspend fun getEvaluationContext(
        @Query("session_id") sessionId: Int,
        @Query("competency_id") competencyId: Int,
        @Query("product_id") productId: Int
    ): EvaluationContext

    // ========== EVALUATION OPERATIONS ==========
    @PUT("evaluation/value")
    suspend fun upsertEvaluation(@Body request: EvalValueRequest): EvaluationResponse

    @GET("evaluation/item")
    suspend fun getEvaluationItem(
        @Query("session_id") sessionId: Int,
        @Query("competency_id") competencyId: Int,
        @Query("ability_id") abilityId: Int,
        @Query("criterion_id") criterionId: Int,
        @Query("product_id") productId: Int,
        @Query("student_id") studentId: Int
    ): EvaluationItem

    @DELETE("evaluation/item")
    suspend fun deleteEvaluationItem(
        @Query("session_id") sessionId: Int,
        @Query("competency_id") competencyId: Int,
        @Query("ability_id") abilityId: Int,
        @Query("criterion_id") criterionId: Int,
        @Query("product_id") productId: Int,
        @Query("student_id") studentId: Int
    ): Response<Unit>

    // ========== MATRIX & REPORTS ==========
    /**
     * ✅ Matriz de evaluación (alternativo)
     */
    @GET("sessions/{session_id}/products/{product_id}/competencies/{competency_id}/matrix")
    suspend fun getEvaluationMatrix(
        @Path("session_id") sessionId: Int,
        @Path("product_id") productId: Int,
        @Path("competency_id") competencyId: Int
    ): MatrixResponse

    /**
     * ✅ CONSOLIDADO - Tu endpoint principal para reportes
     * Usado en ReportsActivity
     */
    @GET("sections/{section_id}/consolidado")
    suspend fun getConsolidatedReport(@Path("section_id") sectionId: Int): ConsolidatedResponse

    // ============================================
    // AUTENTICACIÓN
    // ============================================

    @POST("api/auth/register/alumno")
    suspend fun registerAlumno(
        @Body request: RegisterAlumnoRequest
    ): Response<ApiResponse<UserResponse>>

    @POST("api/auth/register/apoderado")
    suspend fun registerApoderado(
        @Body request: RegisterApoderadoRequest
    ): Response<ApiResponse<UserResponse>>

    @POST("api/auth/register/docente")
    suspend fun registerDocente(
        @Body request: RegisterDocenteRequest
    ): Response<ApiResponse<UserResponse>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(
        @Header("X-Firebase-UID") firebaseUid: String
    ): Response<ApiResponse<UserResponse>>
}