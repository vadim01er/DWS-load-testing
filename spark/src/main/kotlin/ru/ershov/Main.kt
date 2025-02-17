package ru.ershov

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.logging.log4j.core.util.ExecutorServices
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

    val fileStream: Dataset<Row> = spark.readStream()
        .format("text")
        .load(directoryPath)

    val query = fileStream
        .writeStream()
        .outputMode("append")
        .format("console")
        .foreach(SendToKafka())
        .trigger(Trigger.ProcessingTime("5 seconds"))
        .start()

    query.awaitTermination()
    producer.close()
}

private val objectMapper: ObjectMapper = ObjectMapper()

class SendToKafka: ForeachWriter<Row>() {
    override fun open(partitionId: Long, epochId: Long): Boolean {
        return true
    }

    override fun process(value: Row?) {
        println("Received: ${value?.get(0)}")

        val message = value?.get(0).toString()
        objectMapper.readValue(message, List::class.java).forEach {
            val record = ProducerRecord<String, String>("topic", objectMapper.writeValueAsString(it))

            producer.send(record) { metadata, exception ->
                if (exception != null) {
                    println("Ошибка при отправке сообщения: ${exception.message}")
                } else {
                    println("Сообщение отправлено в топик ${metadata.topic()} с смещением ${metadata.offset()}")
                }
            }
        }
    }

    override fun close(errorOrNull: Throwable?) {

    }

}