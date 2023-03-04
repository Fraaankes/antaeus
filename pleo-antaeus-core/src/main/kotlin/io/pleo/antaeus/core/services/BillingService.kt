package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InsufficientFundsException
import io.pleo.antaeus.core.external.MessageHub
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val messageHub: MessageHub,
    private val invoiceService: InvoiceService,
    private val paymentProvider: PaymentProvider
) {
    fun startProcessing() {
        val pendingInvoices = invoiceService.fetchAll(InvoiceStatus.PENDING)
        pendingInvoices.forEach { messageHub.send(messageHub.INVOICE_QUEUE, it.id) }
    }

    fun processInvoice(invoiceId: Int): Unit {
        logger.info("Processing invoice $invoiceId")
        val invoice = invoiceService.fetch(invoiceId);
        if (invoice.status == InvoiceStatus.PENDING){
            val success = paymentProvider.charge(invoice)
            if (success) {
                logger.info("Successfully charged $invoiceId - Updating status")
                invoiceService.updateStatus(invoice, InvoiceStatus.PAID)
            } else {
                throw InsufficientFundsException(invoiceId, invoice.customerId)
            }
        } else {
            logger.info("Invoice $invoiceId is not pending!")
        }
    }
}
