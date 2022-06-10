package quora

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QuoraCloneApplication

fun main(args: Array<String>) {
	runApplication<QuoraCloneApplication>(*args)
}
