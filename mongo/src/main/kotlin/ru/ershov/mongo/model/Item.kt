package ru.ershov.mongo.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.mongodb.core.mapping.Document

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "items")
data class Item(
    val id: Long,
    val name: String,
)