package io.pleo.antaeus.core.external

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class QueueConnectionFactory(private val host: String?) {
    fun getConnection(): Connection {
        val factory = ConnectionFactory()
        if (host != null)
            factory.host = host
        return factory.newConnection()
    }
}