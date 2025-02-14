package ru.ershov

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.ForeachWriter
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import java.util.Properties


val props = Properties().also {
    it["bootstrap.servers"] = "localhost:9092"
    it["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
    it["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
}

val producer = KafkaProducer<String, String>(props)

fun main() {
    val spark = SparkSession.builder()
        .appName("FileWatcherAndSendToKafka")
        .master("local[*]")
        .getOrCreate()

    val directoryPath = "/Users/ruarsv5/Developer/ITMO/sem-3/DWS-load-testing/target" // Укажите путь к директории для отслеживания

    // Создаем поток для чтения новых файлов в директории
    val fileStream: Dataset<Row> = spark.readStream()
        .format("text") // Формат файлов, которые мы ожидаем
        .load(directoryPath)

    // Обработка новых файлов и отправка содержимого в Kafka
    val query = fileStream
        .writeStream()
        .outputMode("append") // Режим добавления
        .format("console") // Выводим данные в консоль
        .foreach(SendToKafka())
        .trigger(Trigger.ProcessingTime("5 seconds")) // Проверяем каждые 5 секунд
        .start()

    query.awaitTermination() // Ожидаем завершения потока
    producer.close()
}


class SendToKafka: ForeachWriter<Row>() {
    override fun open(partitionId: Long, epochId: Long): Boolean {
        return true
    }

    override fun process(value: Row?) {
        println("Received: ${value?.get(0)}")

        val record = ProducerRecord<String, String>("topic", value?.get(0).toString())

        producer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Ошибка при отправке сообщения: ${exception.message}")
            } else {
                println("Сообщение отправлено в топик ${metadata.topic()} с смещением ${metadata.offset()}")
            }
        }

        producer.close() // Закрываем продюсер после отправки
    }

    override fun close(errorOrNull: Throwable?) {

    }

}