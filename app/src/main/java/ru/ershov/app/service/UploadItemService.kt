package ru.ershov.app.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.ershov.app.model.ItemRepository
import java.io.File

private const val DIR = "/Users/ruarsv5/Developer/ITMO/sem-3/DWS-load-testing/target"

@Service
class UploadItemService(
    private val itemRepository: ItemRepository,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "*/10 * * * * *")
    fun saveToFile() {
        val items = itemRepository.findAllByUploadFlagIsFalse()
        if (items.isEmpty()) {
            logger.info("Nothing to save")
            return
        }
        logger.info("Saving file to $DIR")
        val directory = File(DIR)
        if (!directory.exists()) {
            directory.mkdirs() // Создаем директорию, если она не существует
        }

        val existingFiles = directory.listFiles()?.size ?: 0
        val newFileName = "items_${existingFiles + 1}.json"
        val newFile = File(directory, newFileName)

        newFile.writeText(objectMapper.writeValueAsString(items))
        logger.info("Saving file to $DIR/$newFileName")

        items.forEach { it.uploadFlag = true }
        itemRepository.saveAll(items)
    }
}