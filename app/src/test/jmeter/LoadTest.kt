import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.internal.MongoClientImpl
import org.apache.http.entity.ContentType
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import ru.ershov.app.controller.dto.ItemRequestDto
import us.abstracta.jmeter.javadsl.JmeterDsl.*
import us.abstracta.jmeter.javadsl.core.TestPlanStats
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class LoadTest {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val faker = Faker()
    private val objectMapper = ObjectMapper()

    private fun generateRequest(): ItemRequestDto {
        return ItemRequestDto(
            faker.food().ingredient()
        )
    }

    @Test
    fun testRps() {

        // Настройки для потребителя
        val consumerProps = Properties()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = "load-testing"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] =
            StringDeserializer::class.java.getName()
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            StringDeserializer::class.java.getName()
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

        // Создаем потребителя
        val consumer: KafkaConsumer<String, String> = KafkaConsumer(consumerProps)
        val topicPartition = TopicPartition("topic", 0)
        consumer.assign(listOf(topicPartition))
        consumer.seekToEnd(listOf(topicPartition))
        var currentCount =consumer.position(topicPartition)
        println(currentCount)

        val mongoClient: MongoClient =
            MongoClients.create("mongodb://root:example@localhost:27017/admin") // Укажите адрес и порт вашего MongoDB
        val database: MongoDatabase =
            mongoClient.getDatabase("test") // Укажите имя вашей базы данных
        val collection: MongoCollection<Document> =
            database.getCollection("items")

        var currentCountMongo = collection.countDocuments()

        val stats: TestPlanStats = testPlan(
            rpsThreadGroup()
                .maxThreads(100)
                .initThreads(10)
                .rampTo(10.0, Duration.ofSeconds(5))
                .rampTo(1000.0, Duration.ofSeconds(10))
                .rampTo(10_000.0, Duration.ofSeconds(10))
                .rampTo(100_000.0, Duration.ofSeconds(15))
                .children(
                    httpSampler("create item", "http://localhost:8080/api/items")
                        .method(HttpMethod.POST.name())
                        .post({ objectMapper.writeValueAsString(generateRequest()) }, ContentType.APPLICATION_JSON)
                        .children(jsonExtractor("currentItemId", "id")),

                    httpSampler("get created item", "http://localhost:8080/api/items/\${currentItemId}")
                        .method(HttpMethod.GET.name())
                        .contentType(ContentType.APPLICATION_JSON)
                        .children(jsonAssertion("name")),
                ),
            threadGroup(
                1, 4,
                constantTimer(Duration.ofSeconds(10)),
                jsr223Sampler {
                    val listFiles = File("/Users/ruarsv5/Developer/ITMO/sem-3/DWS-load-testing/target").listFiles()!!
                    listFiles.sortBy { it.lastModified() }
                    listFiles.forEach {
                        println(it.name)
                    }
                    val fileCreatedDate =
                        Files.readAttributes(listFiles[listFiles.size - 1].toPath(), BasicFileAttributes::class.java)
                            .creationTime()
                    if (Instant.now().minusSeconds(11).isAfter(fileCreatedDate.toInstant())) {
                        log.error("Service 'app' not create files for 'Spark' service")
                    } else {
                        log.info("Correct save to files")
                    }
                }
            ),
            threadGroup(
                "spark", 1, 4,
                constantTimer(Duration.ofSeconds(14)),
                jsr223Sampler("spark") {
                    consumer.assign(listOf(topicPartition))
                    consumer.seekToEnd(listOf(topicPartition))
                    var count = consumer.position(topicPartition)
                    println("kafka:$count, current: $currentCount")
                    if (count > currentCount) {
                        currentCount = count
                        it.sampleResult.isSuccessful = true
                        log.info("Current send message from 'SPARK'")
                    } else {
                        it.sampleResult.isSuccessful = false
                        log.warn("Нет новых сообщений в топике, вероятно приложение 'SPARK' не пишет в топик")
                    }
                }
            ),

            threadGroup(
                "mongo", 1, 4,
                constantTimer(Duration.ofSeconds(15)),
                jsr223Sampler("mongo") {
                    val mongoClientT: MongoClient =
                        MongoClients.create("mongodb://root:example@localhost:27017/admin") // Укажите адрес и порт вашего MongoDB
                    val databaseT: MongoDatabase =
                        mongoClientT.getDatabase("test") // Укажите имя вашей базы данных

                    val count: Long = databaseT.getCollection("items").countDocuments()
                    log.info("Database $count is > $currentCountMongo")
                    if (count > currentCountMongo) {
                        currentCountMongo = count
                        it.sampleResult.isSuccessful = true
                        log.info("Приложение корректно читает сообщения из кафки")
                    } else {
                        it.sampleResult.isSuccessful = false
                        log.info("Приложение не читает сообщения и не сохраняет")
                    }

                    mongoClientT.close()
                }
            ),

            htmlReporter("target/jmeter/reports"),
        ).run()

        consumer.close()
        mongoClient.close()

    }

}
