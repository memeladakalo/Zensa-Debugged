package com.zensa_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ZensaApiApplication

fun main(args: Array<String>) {
	runApplication<ZensaApiApplication>(*args)
}
