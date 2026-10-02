package dev.scriptor

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue

data object DatabaseController {

    private data class Task(
        val block: () -> Any?,
        val result: CompletableFuture<Any?>,
    )

    private val queue = LinkedBlockingQueue<Task>()

    fun <T> submit(block: () -> T): Future<T> {
        val result = CompletableFuture<Any?>()
        queue.add(Task(block, result))
        return result as Future<T>
    }

    fun run(database: Database) {
        while (!Thread.currentThread().isInterrupted) {
            val task = queue.take()

            try {
                val value = transaction(database) { task.block() }
                task.result.complete(value)
            } catch (e: Exception) {
                task.result.completeExceptionally(e)
            }
        }
    }
}

fun <T> db(block: () -> T): T {
    return DatabaseController.submit(block).get()
}
