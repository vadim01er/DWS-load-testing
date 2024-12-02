package ru.ershov.app.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ItemRepository: JpaRepository<Item, Long> {
    fun findAllByUploadFlagIsFalse(): List<Item>
}