package io.pleo.antaeus.core.external

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

class MessageHub(private val connection: Connection) {
    val INVOICE_QUEUE get() = "invoices"
    val INVOICE_DEADLETTER_QUEUE get() = "invoices-deadletter"



    fun send(queue: String, body: Any, headers: Map<String, Any> = HashMap()) {
        val channel = getChannel()
        val properties = AMQP.BasicProperties.Builder()
            .headers(headers.toMap())
            .build()
        // Use the default exchange
        channel.basicPublish("", queue, properties, body.toString().toByteArray())
    }

    fun getChannel(): Channel = connection.createChannel()
}