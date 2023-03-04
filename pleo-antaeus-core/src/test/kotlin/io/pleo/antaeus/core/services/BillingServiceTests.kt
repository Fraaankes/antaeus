package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.MessageHub
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTests {
    private val _pendingInvoices = listOf(
        Invoice(
            1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING
        ),
        Invoice(2, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING),
        Invoice(3, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
    )

    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
        every { fetchInvoices(InvoiceStatus.PENDING) } returns _pendingInvoices
        every { fetchInvoice(404) } returns null
        every { fetchInvoice(1337) } returns Invoice(1337, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PAID)
        every { fetchInvoice(42) } returns Invoice(42, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        every { updateStatus(any(), any()) } returns Unit
    }
    private val invoiceService = InvoiceService(dal = dal)
    private val paymentService = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val hub = mockk<MessageHub> {
        every { send(any(), any(), any()) } returns Unit
        every { INVOICE_QUEUE } answers { "queue" }
    }
    private val billingService = BillingService(hub, invoiceService, paymentService)

    @Test
    fun `will enqueue all pending invoices when starting`() {
        billingService.startProcessing()
        verify {
            hub.send("queue", 1, any())
        }
        verify {
            hub.send("queue", 2, any())
        }
        verify {
            hub.send("queue", 3, any())
        }
    }

    @Test
    fun `will rethrow from invoice service`() {
        assertThrows<InvoiceNotFoundException> { billingService.processInvoice(404) }
    }

    @Test
    fun `will skip a paid invoice`() {
        billingService.processInvoice(1337)
        verify(exactly = 0) {
            invoiceService.updateStatus(any(), any())
        }
    }

    @Test
    fun `will update a processed invoice`() {
        billingService.processInvoice(42)
        verify(exactly = 1) {
            invoiceService.updateStatus(any(), InvoiceStatus.PAID)
        }
    }
}