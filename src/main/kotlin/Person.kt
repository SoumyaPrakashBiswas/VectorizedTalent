package com.example.personvectorsearch

import kotlinx.serialization.Serializable

@Serializable
data class Person(
    val ID: String,
    val Bio: String,
    val Resume_html: String,
    val Category: String,
    val embedding_index: Int = -1
)