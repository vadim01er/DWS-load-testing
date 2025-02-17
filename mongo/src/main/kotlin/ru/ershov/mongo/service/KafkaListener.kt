package ru.ershov.mongo.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.ershov.mongo.model.Item
import ru.ershov.mongo.model.ItemRepository

@Service
@Transactional
class KafkaListener(
    private val itemRepository: ItemRepository,
    private val objectMapper: ObjectMapper
) {
    @KafkaListener(topics = ["topic"], groupId = "mongo")
    fun listen(data: String) {
        println("Received data: $data")
        val item = objectMapper.readValue<Item>(data)
        itemRepository.save(item)
    }
}