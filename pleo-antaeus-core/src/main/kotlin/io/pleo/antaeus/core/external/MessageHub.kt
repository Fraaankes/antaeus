package io.pleo.antaeus.core.external

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import java.util.UUID

class MessageHub(private val connection: Connection) {
    val RETRY_HEADER get() = "retryCount"
    val INVOICE_QUEUE get() = "invoices-queue"
    val INVOICE_DEADLETTER_QUEUE get() = "invoices-deadletter"



    fun send(queue: String, body: Any, headers: Map<String, Any> = HashMap()) {
        val channel = getChannel()
        val properties = AMQP.BasicProperties.Builder()
            .messageId(UUID.randomUUID().toString())
            .headers(headers.toMap())
            .build()
        // Use the default exchange
        channel.basicPublish("", queue, properties, body.toString().toByteArray())
    }

    fun getChannel(): Channel = connection.createChannel()
    fun ack(channel: Channel, deliveryTag: Long) {
        channel.basicAck(deliveryTag, false)
    }

    fun nack(channel: Channel, deliveryTag: Long)  {
        channel.basicNack(deliveryTag, false, false)
    }
}