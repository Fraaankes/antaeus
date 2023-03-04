package io.pleo.antaeus.core.invoicing

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.pleo.antaeus.core.exceptions.InsufficientFundsException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.MessageHub
import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PayableInvoiceListener(private val messageHub: MessageHub, private val billingService: BillingService) {
    fun listen() {
        val channel = messageHub.getChannel()
        val messageHandler = PayableInvoiceHandler(channel, messageHub, billingService)
        channel.basicConsume(messageHub.INVOICE_QUEUE, false, messageHandler)
    }

    private class PayableInvoiceHandler(
        channel: Channel, private val messageHub: MessageHub, private val billingService: BillingService
    ) : DefaultConsumer(channel) {

        override fun handleDelivery(
            consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?
        ) {
            if (body == null) throw IllegalArgumentException("No body in message")

            val invoiceId = String(body, charset("UTF-8")).toInt()
            val retryCount = properties?.headers?.get(messageHub.RETRY_HEADER)?.toString()?.toInt() ?: 1

            // Configurable? Should probably be
            if (retryCount > 3) {
                logger.info { "Msg id ${properties?.messageId} has exceeded the retryCount of 3. Current value $retryCount" }
                sendToDeadletterQueue(envelope, invoiceId)
                return
            }

            try {
                logger.info { "Handling msg with id ${properties?.messageId}" }
                billingService.processInvoice(invoiceId)
                envelope?.deliveryTag?.let { messageHub.ack(channel, it) }
            } catch (e: Exception) {
                logger.error { "Msg with id ${properties?.messageId} failed with $e" }

                val shouldRetry = e is InsufficientFundsException || e is NetworkException
                if (shouldRetry) {
                    retry(envelope, invoiceId, retryCount + 1)
                } else {
                    sendToDeadletterQueue(envelope, invoiceId)
                }
            }
        }

        private fun sendToDeadletterQueue(envelope: Envelope?, invoiceId: Int) {
            messageHub.send(messageHub.INVOICE_DEADLETTER_QUEUE, invoiceId)
            envelope?.deliveryTag?.let { messageHub.nack(channel, it) }
        }

        private fun retry(envelope: Envelope?, invoiceId: Int, retryCount: Int) {
            val header = mapOf(messageHub.RETRY_HEADER to retryCount)
            messageHub.send(messageHub.INVOICE_QUEUE, invoiceId, header)

            // Only ack after publishing was a success
            envelope?.deliveryTag?.let { messageHub.ack(channel, it) }
        }
    }
}