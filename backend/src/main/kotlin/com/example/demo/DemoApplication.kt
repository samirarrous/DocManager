package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
	loadEnv()
	runApplication<DemoApplication>(*args)
}

private fun loadEnv() {
	val files = listOf(File(".env"), File("backend/.env"), File("../.env"))
	for (file in files) {
		if (file.exists() && file.isFile) {
			file.useLines { lines ->
				lines.forEach { line ->
					val trimmed = line.trim()
					if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
						val parts = trimmed.split("=", limit = 2)
						if (parts.size == 2) {
							val key = parts[0].trim()
							val value = parts[1].trim()
								.removeSurrounding("\"")
								.removeSurrounding("'")
							System.setProperty(key, value)
						}
					}
				}
			}
			break
		}
	}
}

