package ru.ershov.app.controller.dto

data class ItemResponseDto(
    val id: Long? = null,
    val name: String,
    val uploadFlag: Boolean = false,
)