package com.evalua.classter.network

data class CreateStudentRequest(
    val full_name: String
)

data class BatchStudentsRequest(
    val students: List<CreateStudentRequest>
)

data class ImportStudentsResponse(
    val imported: Int,
    val successes: List<String>,
    val errors: List<String>
)

data class CreateGradeRequest(
    val number: Int
)

data class CreateSectionRequest(
    val letter: String? = null
)