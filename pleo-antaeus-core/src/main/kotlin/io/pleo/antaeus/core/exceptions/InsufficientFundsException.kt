package io.pleo.antaeus.core.exceptions

class InsufficientFundsException(invoiceId: Int, customerId: Int) :
    Exception("Invoice '$invoiceId' cannot be paid by '$customerId'")