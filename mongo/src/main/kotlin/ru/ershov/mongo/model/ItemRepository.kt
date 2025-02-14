package ru.ershov.mongo.model

import org.springframework.data.mongodb.repository.MongoRepository

interface ItemRepository: MongoRepository<Item, Long> {
}