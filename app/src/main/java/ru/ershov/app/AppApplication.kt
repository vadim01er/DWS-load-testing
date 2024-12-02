package ru.ershov.app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class AppApplication

fun main(args: Array<String>) {
    SpringApplication.run(AppApplication::class.java, *args)
}

