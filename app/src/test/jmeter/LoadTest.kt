import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import ru.ershov.app.controller.dto.ItemRequestDto
import us.abstracta.jmeter.javadsl.JmeterDsl.*
import us.abstracta.jmeter.javadsl.core.TestPlanStats
import java.time.Duration


class LoadTest {

    private val faker = Faker()
    private val objectMapper = ObjectMapper()

    private fun generateRequest(): ItemRequestDto {
        return ItemRequestDto(
            faker.food().ingredient()
        )
    }

    @Test
    fun testRps() {
        val stats: TestPlanStats = testPlan(
            rpsThreadGroup()
                .maxThreads(100)
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
            htmlReporter("target/jmeter/reports"),
        ).run()

        assertThat(stats.overall().sampleTimePercentile99()).isLessThan(Duration.ofMillis(500))

    }

}