import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random
import io.pleo.antaeus.core.external.MessageHub

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)), currency = customer.currency
                ), customer = customer, status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if (Random.nextInt(1, 5) == 1){
                throw NetworkException()
            }
            return Random.nextBoolean()
        }
    }
}

internal fun setupQueues(messageHub: MessageHub) {
    val channel = messageHub.getChannel();
    channel.queueDeclare(messageHub.INVOICE_QUEUE, false, false, false, null)
    channel.queueDeclare(messageHub.INVOICE_DEADLETTER_QUEUE, false, false, false, null)
}