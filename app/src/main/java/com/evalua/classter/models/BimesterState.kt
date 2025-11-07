package com.evalua.classter.models

data class BimesterState(
    var isExpanded: Boolean = false,
    val gradeStates: MutableMap<Int, Boolean> = mutableMapOf() // gradeId -> isExpanded
)