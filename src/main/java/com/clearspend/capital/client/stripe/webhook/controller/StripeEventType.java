package com.clearspend.capital.client.stripe.webhook.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// From: https://stripe.com/docs/api/events/types
public enum StripeEventType {
  UNKNOWN("", "placeholder to prevent things from getting too explodey"),
  ACCOUNT_UPDATED(
      "account.updated",
      "Occurs whenever an account status or property has changed. [data.object is an account]"),
  ACCOUNT_APPLICATION_AUTHORIZED(
      "account.application.authorized",
      "Occurs whenever a user authorizes an application. Sent to the related application only."
          + " [data.object is an 'application']"),
  ACCOUNT_APPLICATION_DEAUTHORIZED(
      "account.application.deauthorized",
      "Occurs whenever a user deauthorizes an application. Sent to the related application only. "
          + " [data.object is an 'application']"),
  ACCOUNT_EXTERNAL_ACCOUNT_CREATED(
      "account.external_account.created",
      "Occurs whenever an external account is created. [data.object is an external account (e.g., "
          + " card or bank account)]"),
  ACCOUNT_EXTERNAL_ACCOUNT_DELETED(
      "account.external_account.deleted",
      "Occurs whenever an external account is deleted. [data.object is an external account (e.g., "
          + " card or bank account)]"),
  ACCOUNT_EXTERNAL_ACCOUNT_UPDATED(
      "account.external_account.updated",
      "Occurs whenever an external account is updated. [data.object is an external account (e.g., "
          + " card or bank account)]"),
  APPLICATION_FEE_CREATED(
      "application_fee.created",
      "Occurs whenever an application fee is created on a charge. [data.object is an application "
          + " fee]"),
  APPLICATION_FEE_REFUNDED(
      "application_fee.refunded",
      "Occurs whenever an application fee is refunded, whether from refunding a charge or from "
          + " refunding the application fee directly. This includes partial refunds. [data.object is an "
          + " application fee]"),
  APPLICATION_FEE_REFUND_UPDATED(
      "application_fee.refund.updated",
      "Occurs whenever an application fee refund is updated. [data.object is a fee refund]"),
  BALANCE_AVAILABLE(
      "balance.available",
      "Occurs whenever your Stripe balance has been updated (e.g., when a charge is available to "
          + " be paid out). By default, Stripe automatically transfers funds in your balance to your bank "
          + " account on a daily basis. [data.object is a balance]"),
  BILLING_PORTAL_CONFIGURATION_CREATED(
      "billing_portal.configuration.created",
      "Occurs whenever a portal configuration is created. [data.object is a portal "
          + " configuration]"),
  BILLING_PORTAL_CONFIGURATION_UPDATED(
      "billing_portal.configuration.updated",
      "Occurs whenever a portal configuration is updated. [data.object is a portal "
          + " configuration]"),
  CAPABILITY_UPDATED(
      "capability.updated",
      "Occurs whenever a capability has new requirements or a new status. [data.object is a "
          + " capability]"),
  CHARGE_CAPTURED(
      "charge.captured",
      "Occurs whenever a previously uncaptured charge is captured. [data.object is a charge]"),
  CHARGE_EXPIRED(
      "charge.expired", "Occurs whenever an uncaptured charge expires. [data.object is a charge]"),
  CHARGE_FAILED(
      "charge.failed", "Occurs whenever a failed charge attempt occurs. [data.object is a charge]"),
  CHARGE_PENDING(
      "charge.pending", "Occurs whenever a pending charge is created. [data.object is a charge]"),
  CHARGE_REFUNDED(
      "charge.refunded",
      "Occurs whenever a charge is refunded, including partial refunds. [data.object is a charge]"),
  CHARGE_SUCCEEDED(
      "charge.succeeded", "Occurs whenever a charge is successful. [data.object is a charge]"),
  CHARGE_UPDATED(
      "charge.updated",
      "Occurs whenever a charge description or metadata is updated. [data.object is a charge]"),
  CHARGE_DISPUTE_CLOSED(
      "charge.dispute.closed",
      "Occurs when a dispute is closed and the dispute status changes to lost, warning_closed, or "
          + "won. [data.object is a dispute]"),
  CHARGE_DISPUTE_CREATED(
      "charge.dispute.created",
      "Occurs whenever a customer disputes a charge with their bank. [data.object is a dispute]"),
  CHARGE_DISPUTE_FUNDS_REINSTATED(
      "charge.dispute.funds_reinstated",
      "Occurs when funds are reinstated to your account after a dispute is closed. This includes "
          + "partially refunded payments. [data.object is a dispute]"),
  CHARGE_DISPUTE_FUNDS_WITHDRAWN(
      "charge.dispute.funds_withdrawn",
      "Occurs when funds are removed from your account due to a dispute. [data.object is a "
          + "dispute]"),
  CHARGE_DISPUTE_UPDATED(
      "charge.dispute.updated",
      "Occurs when the dispute is updated (usually with evidence). [data.object is a dispute]"),
  CHARGE_REFUND_UPDATED(
      "charge.refund.updated",
      "Occurs whenever a refund is updated, on selected payment methods. [data.object is a "
          + "refund]"),
  CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED(
      "checkout.session.async_payment_failed",
      "Occurs when a payment intent using a delayed payment method fails. [data.object is a "
          + "checkout session]"),
  CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED(
      "checkout.session.async_payment_succeeded",
      "Occurs when a payment intent using a delayed payment method finally succeeds. [data.object "
          + "is a checkout session]"),
  CHECKOUT_SESSION_COMPLETED(
      "checkout.session.completed",
      "Occurs when a Checkout Session has been successfully completed. [data.object is a checkout "
          + "session]"),
  CHECKOUT_SESSION_EXPIRED(
      "checkout.session.expired",
      "Occurs when a Checkout Session is expired. [data.object is a checkout session]"),
  COUPON_CREATED(
      "coupon.created", "Occurs whenever a coupon is created. [data.object is a coupon]"),
  COUPON_DELETED(
      "coupon.deleted", "Occurs whenever a coupon is deleted. [data.object is a coupon]"),
  COUPON_UPDATED(
      "coupon.updated", "Occurs whenever a coupon is updated. [data.object is a coupon]"),
  CREDIT_NOTE_CREATED(
      "credit_note.created",
      "Occurs whenever a credit note is created. [data.object is a credit note]"),
  CREDIT_NOTE_UPDATED(
      "credit_note.updated",
      "Occurs whenever a credit note is updated. [data.object is a credit note]"),
  CREDIT_NOTE_VOIDED(
      "credit_note.voided",
      "Occurs whenever a credit note is voided. [data.object is a credit note]"),
  CREDIT_REVERSAL_CREATED(
      "credit_reversal.created",
      "Occurs whenever an CreditReversal is submitted and created. [data.object is a credit "
          + "reversal]"),
  CREDIT_REVERSAL_POSTED(
      "credit_reversal.posted",
      "Occurs whenever an CreditReversal post is posted. [data.object is a credit reversal]"),
  CUSTOMER_CREATED(
      "customer.created",
      "Occurs whenever a new customer is created. [data.object is a " + "customer]"),
  CUSTOMER_DELETED(
      "customer.deleted", "Occurs whenever a customer is deleted. [data.object is a customer]"),
  CUSTOMER_UPDATED(
      "customer.updated",
      "Occurs whenever any property of a customer changes. [data.object is a customer]"),
  CUSTOMER_DISCOUNT_CREATED(
      "customer.discount.created",
      "Occurs whenever a coupon is attached to a customer. [data.object is a discount]"),
  CUSTOMER_DISCOUNT_DELETED(
      "customer.discount.deleted",
      "Occurs whenever a coupon is removed from a customer. [data.object is a discount]"),
  CUSTOMER_DISCOUNT_UPDATED(
      "customer.discount.updated",
      "Occurs whenever a customer is switched from one coupon to another. [data.object is a "
          + "discount]"),
  CUSTOMER_SOURCE_CREATED(
      "customer.source.created",
      "Occurs whenever a new source is created for a customer. [data.object is a source (e.g., card)]"),
  CUSTOMER_SOURCE_DELETED(
      "customer.source.deleted",
      "Occurs whenever a source is removed from a customer. [data.object is a source (e.g., "
          + "card)]"),
  CUSTOMER_SOURCE_EXPIRING(
      "customer.source.expiring",
      "Occurs whenever a card or source will expire at the end of the month. [data.object is a "
          + "source (e.g., card)]"),
  CUSTOMER_SOURCE_UPDATED(
      "customer.source.updated",
      "Occurs whenever a source's details are changed. [data.object is a source (e.g., card)]"),
  CUSTOMER_SUBSCRIPTION_CREATED(
      "customer.subscription.created",
      "Occurs whenever a customer is signed up for a new plan. [data.object is a subscription]"),
  CUSTOMER_SUBSCRIPTION_DELETED(
      "customer.subscription.deleted",
      "Occurs whenever a customer's subscription ends. [data.object is a subscription]"),
  CUSTOMER_SUBSCRIPTION_PENDING_UPDATE_APPLIED(
      "customer.subscription.pending_update_applied",
      "Occurs whenever a customer's subscription's pending update is applied, and the subscription "
          + "is updated. [data.object is a subscription]"),
  CUSTOMER_SUBSCRIPTION_PENDING_UPDATE_EXPIRED(
      "customer.subscription.pending_update_expired",
      "Occurs whenever a customer's subscription's pending update expires before the related "
          + "invoice is paid. [data.object is a subscription]"),
  CUSTOMER_SUBSCRIPTION_TRIAL_WILL_END(
      "customer.subscription.trial_will_end",
      "Occurs three days before a subscription's trial period is scheduled to end, or when a trial "
          + "is ended immediately (using trial_end=now). [data.object is a subscription]"),
  CUSTOMER_SUBSCRIPTION_UPDATED(
      "customer.subscription.updated",
      "Occurs whenever a subscription changes (e.g., switching from one plan to another, or "
          + "changing the status from trial to active). [data.object is a subscription]"),
  CUSTOMER_TAX_ID_CREATED(
      "customer.tax_id.created",
      "Occurs whenever a tax ID is created for a customer. [data.object is a tax id]"),
  CUSTOMER_TAX_ID_DELETED(
      "customer.tax_id.deleted",
      "Occurs whenever a tax ID is deleted from a customer. [data.object is a tax id]"),
  CUSTOMER_TAX_ID_UPDATED(
      "customer.tax_id.updated",
      "Occurs whenever a customer's tax ID is updated. [data.object is a tax id]"),
  DEBIT_REVERSAL_COMPLETED(
      "debit_reversal.completed",
      "Occurs whenever a DebitReversal is completed. [data.object is a debit reversal]"),
  DEBIT_REVERSAL_CREATED(
      "debit_reversal.created",
      "Occurs whenever a DebitReversal is created. [data.object is a debit reversal]"),
  FILE_CREATED(
      "file.created",
      "Occurs whenever a new Stripe-generated file is available for your account. [data.object is "
          + "a file]"),
  FINANCIAL_ACCOUNT_CLOSED(
      "financial_account.closed",
      "Occurs whenever the status of the FinancialAccount becomes closed. [data.object is a "
          + "financial account]"),
  FINANCIAL_ACCOUNT_CREATED(
      "financial_account.created",
      "Occurs whenever a new FinancialAccount is created. [data.object is a financial account]"),
  FINANCIAL_ACCOUNT_FEATURES_STATUS_UPDATED(
      "financial_account.features_status_updated",
      "Occurs whenever the statuses of any features within an existing FinancialAccount are "
          + "updated. [data.object is a financial account]"),
  FINANCIAL_ACCOUNT_STATUS_UPDATED(
      "financial_account.status_updated",
      "Occurs whenever any of the statuses within an existing FinancialAccount are updated. "
          + "[data.object is a financial account]"),
  IDENTITY_VERIFICATION_SESSION_CANCELED(
      "identity.verification_session.canceled",
      "Occurs whenever a VerificationSession is canceled [data.object is a verification "
          + "session]"),
  IDENTITY_VERIFICATION_SESSION_CREATED(
      "identity.verification_session.created",
      "Occurs whenever a VerificationSession is created [data.object is a verification session]"),
  IDENTITY_VERIFICATION_SESSION_PROCESSING(
      "identity.verification_session.processing",
      "Occurs whenever a VerificationSession transitions to processing [data.object is a "
          + "verification session]"),
  IDENTITY_VERIFICATION_SESSION_REDACTED(
      "identity.verification_session.redacted",
      "Occurs whenever a VerificationSession is redacted. You must create a webhook endpoint which "
          + "explicitly subscribes to this event type to access it. Webhook endpoints which subscribe to "
          + "all events will not include this event type. [data.object is a verification session]"),
  IDENTITY_VERIFICATION_SESSION_REQUIRES_INPUT(
      "identity.verification_session.requires_input",
      "Occurs whenever a VerificationSession transitions to require user input [data.object is a "
          + "verification session]"),
  IDENTITY_VERIFICATION_SESSION_VERIFIED(
      "identity.verification_session.verified",
      "Occurs whenever a VerificationSession transitions to verified [data.object is a "
          + "verification session]"),
  INBOUND_TRANSFER_CREATED(
      "inbound_transfer.created",
      "Occurs whenever an InboundTransfer is created. [data.object is an inbound transfer]"),
  INBOUND_TRANSFER_FAILED(
      "inbound_transfer.failed",
      "Occurs whenever an InboundTransfer has failed. [data.object is an inbound transfer]"),
  INBOUND_TRANSFER_SUCCEEDED(
      "inbound_transfer.succeeded",
      "Occurs whenever an InboundTransfer has succeeded. [data.object is an inbound transfer]"),
  INVOICE_CREATED(
      "invoice.created",
      "Occurs whenever a new invoice is created. To learn how webhooks can be used with this "
          + "event, and how they can affect it, see Using Webhooks with Subscriptions. [data.object is an "
          + "invoice]"),
  INVOICE_DELETED(
      "invoice.deleted", "Occurs whenever a draft invoice is deleted. [data.object is an invoice]"),
  INVOICE_FINALIZATION_FAILED(
      "invoice.finalization_failed",
      "Occurs whenever a draft invoice cannot be finalized. See the invoice’s last finalization "
          + "error for details. [data.object is an invoice]"),
  INVOICE_FINALIZED(
      "invoice.finalized",
      "Occurs whenever a draft invoice is finalized and updated to be an open invoice. "
          + "[data.object is an invoice]"),
  INVOICE_MARKED_UNCOLLECTIBLE(
      "invoice.marked_uncollectible",
      "Occurs whenever an invoice is marked uncollectible. [data.object is an invoice]"),
  INVOICE_PAID(
      "invoice.paid",
      "Occurs whenever an invoice payment attempt succeeds or an invoice is marked as paid "
          + "out-of-band. [data.object is an invoice]"),
  INVOICE_PAYMENT_ACTION_REQUIRED(
      "invoice.payment_action_required",
      "Occurs whenever an invoice payment attempt requires further user action to complete. "
          + "[data.object is an invoice]"),
  INVOICE_PAYMENT_FAILED(
      "invoice.payment_failed",
      "Occurs whenever an invoice payment attempt fails, due either to a declined payment or to "
          + "the lack of a stored payment method. [data.object is an invoice]"),
  INVOICE_PAYMENT_SUCCEEDED(
      "invoice.payment_succeeded",
      "Occurs whenever an invoice payment attempt succeeds. [data.object is an invoice]"),
  INVOICE_SENT(
      "invoice.sent", "Occurs whenever an invoice email is sent out. [data.object is an invoice]"),
  INVOICE_UPCOMING(
      "invoice.upcoming",
      "Occurs X number of days before a subscription is scheduled to create an invoice that is "
          + "automatically charged—where X is determined by your subscriptions settings. Note: The "
          + "received Invoice object will not have an invoice ID. [data.object is an invoice]"),
  INVOICE_UPDATED(
      "invoice.updated",
      "Occurs whenever an invoice changes (e.g., the invoice amount). [data.object is an "
          + "invoice]"),
  INVOICE_VOIDED(
      "invoice.voided", "Occurs whenever an invoice is voided. [data.object is an invoice]"),
  INVOICEITEM_CREATED(
      "invoiceitem.created",
      "Occurs whenever an invoice item is created. [data.object is an invoiceitem]"),
  INVOICEITEM_DELETED(
      "invoiceitem.deleted",
      "Occurs whenever an invoice item is deleted. [data.object is an invoiceitem]"),
  INVOICEITEM_UPDATED(
      "invoiceitem.updated",
      "Occurs whenever an invoice item is updated. [data.object is an invoiceitem]"),
  ISSUING_AUTHORIZATION_CREATED(
      "issuing_authorization.created",
      "Occurs whenever an authorization is created. [data.object is an issuing authorization]"),
  ISSUING_AUTHORIZATION_REQUEST(
      "issuing_authorization.request",
      "Represents a synchronous request for authorization, see Using your integration to handle "
          + "authorization requests. You must create a webhook endpoint which explicitly subscribes to "
          + "this event type to access it. Webhook endpoints which subscribe to all events will not "
          + "include this event type. [data.object is an issuing authorization]"),
  ISSUING_AUTHORIZATION_UPDATED(
      "issuing_authorization.updated",
      "Occurs whenever an authorization is updated. [data.object is an issuing authorization]"),
  ISSUING_CARD_CREATED(
      "issuing_card.created",
      "Occurs whenever a card is created. [data.object is an issuing card]"),
  ISSUING_CARD_UPDATED(
      "issuing_card.updated",
      "Occurs whenever a card is updated. [data.object is an issuing card]"),
  ISSUING_CARDHOLDER_CREATED(
      "issuing_cardholder.created",
      "Occurs whenever a cardholder is created. [data.object is an issuing cardholder]"),
  ISSUING_CARDHOLDER_UPDATED(
      "issuing_cardholder.updated",
      "Occurs whenever a cardholder is updated. [data.object is an issuing cardholder]"),
  ISSUING_DISPUTE_CLOSED(
      "issuing_dispute.closed",
      "Occurs whenever a dispute is won, lost or expired. [data.object is an issuing dispute]"),
  ISSUING_DISPUTE_CREATED(
      "issuing_dispute.created",
      "Occurs whenever a dispute is created. [data.object is an issuing dispute]"),
  ISSUING_DISPUTE_FUNDS_REINSTATED(
      "issuing_dispute.funds_reinstated",
      "Occurs whenever funds are reinstated to your account for an Issuing dispute. [data.object "
          + "is an issuing dispute]"),
  ISSUING_DISPUTE_SUBMITTED(
      "issuing_dispute.submitted",
      "Occurs whenever a dispute is submitted. [data.object is an issuing dispute]"),
  ISSUING_DISPUTE_UPDATED(
      "issuing_dispute.updated",
      "Occurs whenever a dispute is updated. [data.object is an issuing dispute]"),
  ISSUING_TRANSACTION_CREATED(
      "issuing_transaction.created",
      "Occurs whenever an issuing transaction is created. [data.object is an issuing transaction]"),
  ISSUING_TRANSACTION_UPDATED(
      "issuing_transaction.updated",
      "Occurs whenever an issuing transaction is updated. [data.object is an issuing transaction]"),
  LINKED_ACCOUNT_CREATED(
      "linked_account.created",
      "Occurs when a new linked account is created. [data.object is a linked account]"),
  LINKED_ACCOUNT_DEACTIVATED(
      "linked_account.deactivated",
      "Occurs when a linked account's status is updated from ACTIVE to INACTIVE. [data.object is a "
          + "linked account]"),
  LINKED_ACCOUNT_REACTIVATED(
      "linked_account.reactivated",
      "Occurs when a linked account's status is updated from INACTIVE to ACTIVE. [data.object is a "
          + "linked account]"),
  LINKED_ACCOUNT_REFRESHED_BALANCE(
      "linked_account.refreshed_balance",
      "Occurs when a linked account's balance refresh completes, successfully or unsuccessfully.. "
          + "[data.object is a linked account]"),
  MANDATE_UPDATED(
      "mandate.updated", "Occurs whenever a Mandate is updated. [data.object is a mandate]"),
  ORDER_CREATED("order.created", "Occurs whenever an order is created. [data.object is an order]"),
  ORDER_PAYMENT_FAILED(
      "order.payment_failed",
      "Occurs whenever an order payment attempt fails. [data.object is an order]"),
  ORDER_PAYMENT_SUCCEEDED(
      "order.payment_succeeded",
      "Occurs whenever an order payment attempt succeeds. [data.object is an order]"),
  ORDER_UPDATED("order.updated", "Occurs whenever an order is updated. [data.object is an order]"),
  ORDER_RETURN_CREATED(
      "order_return.created",
      "Occurs whenever an order return is created. [data.object is an order return]"),
  OUTBOUND_PAYMENT_CANCELED(
      "outbound_payment.canceled",
      "Occurs whenever an OutboundPayment is canceled. [data.object is an outbound payment]"),
  OUTBOUND_PAYMENT_CREATED(
      "outbound_payment.created",
      "Occurs whenever a new OutboundPayment is successfully created. [data.object is an outbound payment]"),
  OUTBOUND_PAYMENT_EXPECTED_ARRIVAL_DATE_UPDATED(
      "outbound_payment.expected_arrival_date_updated",
      "Occurs whenever the arrival date on an OutboundPayment updates. [data.object is an outbound payment]"),
  OUTBOUND_PAYMENT_FAILED(
      "outbound_payment.failed",
      "Occurs whenever an OutboundPayment fails. [data.object is an outbound payment]"),
  OUTBOUND_PAYMENT_POSTED(
      "outbound_payment.posted",
      "Occurs whenever an OutboundPayment posts. [data.object is an outbound payment]"),
  OUTBOUND_PAYMENT_PROCESSING(
      "outbound_payment.processing",
      "Occurs whenever an OutboundPayment is submitted and being processed. [data.object is an outbound payment]"),
  OUTBOUND_PAYMENT_RETURNED(
      "outbound_payment.returned",
      "Occurs whenever an OutboundPayment was returned. [data.object is an outbound payment]"),
  OUTBOUND_TRANSFER_CANCELED(
      "outbound_transfer.canceled",
      "Occurs whenever an OutboundTransfer is canceled. [data.object is an outbound transfer]"),
  OUTBOUND_TRANSFER_CREATED(
      "outbound_transfer.created",
      "Occurs whenever an OutboundTransfer is created. [data.object is an outbound transfer]"),
  OUTBOUND_TRANSFER_EXPECTED_ARRIVAL_DATE_UPDATED(
      "outbound_transfer.expected_arrival_date_updated",
      "Occurs whenever the arrival date on an OutboundTransfer updates. [data.object is an outbound transfer]"),
  OUTBOUND_TRANSFER_FAILED(
      "outbound_transfer.failed",
      "Occurs whenever an OutboundTransfer has failed. [data.object is an outbound transfer]"),
  OUTBOUND_TRANSFER_POSTED(
      "outbound_transfer.posted",
      "Occurs whenever an OutboundTransfer is posted. [data.object is an outbound transfer]"),
  OUTBOUND_TRANSFER_RETURNED(
      "outbound_transfer.returned",
      "Occurs whenever an OutboundTransfer is returned. [data.object is an outbound transfer]"),
  PAYMENT_INTENT_AMOUNT_CAPTURABLE_UPDATED(
      "payment_intent.amount_capturable_updated",
      "Occurs when a PaymentIntent has funds to be captured. Check the amount_capturable property "
          + "on the PaymentIntent to determine the amount that can be captured. You may capture the "
          + "PaymentIntent with an amount_to_capture value up to the specified amount. Learn more about "
          + "capturing PaymentIntents. [data.object is a payment intent]"),
  PAYMENT_INTENT_CANCELED(
      "payment_intent.canceled",
      "Occurs when a PaymentIntent is canceled. [data.object is a payment intent]"),
  PAYMENT_INTENT_CREATED(
      "payment_intent.created",
      "Occurs when a new PaymentIntent is created. [data.object is a payment intent]"),
  PAYMENT_INTENT_PAYMENT_FAILED(
      "payment_intent.payment_failed",
      "Occurs when a PaymentIntent has failed the attempt to create a payment method or a payment. "
          + "[data.object is a payment intent]"),
  PAYMENT_INTENT_PROCESSING(
      "payment_intent.processing",
      "Occurs when a PaymentIntent has started processing. [data.object is a payment intent]"),
  PAYMENT_INTENT_REQUIRES_ACTION(
      "payment_intent.requires_action",
      "Occurs when a PaymentIntent transitions to requires_action state [data.object is a payment intent]"),
  PAYMENT_INTENT_SUCCEEDED(
      "payment_intent.succeeded",
      "Occurs when a PaymentIntent has successfully completed payment. [data.object is a payment intent]"),
  PAYMENT_METHOD_ATTACHED(
      "payment_method.attached",
      "Occurs whenever a new payment method is attached to a customer. [data.object is a payment method]"),
  PAYMENT_METHOD_AUTOMATICALLY_UPDATED(
      "payment_method.automatically_updated",
      "Occurs whenever a payment method's details are automatically updated by the network. "
          + "[data.object is a payment method]"),
  PAYMENT_METHOD_DETACHED(
      "payment_method.detached",
      "Occurs whenever a payment method is detached from a customer. [data.object is a payment method]"),
  PAYMENT_METHOD_UPDATED(
      "payment_method.updated",
      "Occurs whenever a payment method is updated via the PaymentMethod update API. [data.object "
          + "is a payment method]"),
  PAYOUT_CANCELED(
      "payout.canceled", "Occurs whenever a payout is canceled. [data.object is a payout]"),
  PAYOUT_CREATED(
      "payout.created", "Occurs whenever a payout is created. [data.object is a payout]"),
  PAYOUT_FAILED(
      "payout.failed", "Occurs whenever a payout attempt fails. [data.object is a payout]"),
  PAYOUT_PAID(
      "payout.paid",
      "Occurs whenever a payout is expected to be available in the destination account. If the "
          + "payout fails, a payout.failed notification is also sent, at a later time. [data.object is a "
          + "payout]"),
  PAYOUT_UPDATED(
      "payout.updated", "Occurs whenever a payout is updated. [data.object is a payout]"),
  PERSON_CREATED(
      "person.created",
      "Occurs whenever a person associated with an account is created. [data.object is a person]"),
  PERSON_DELETED(
      "person.deleted",
      "Occurs whenever a person associated with an account is deleted. [data.object is a person]"),
  PERSON_UPDATED(
      "person.updated",
      "Occurs whenever a person associated with an account is updated. [data.object is a person]"),
  PLAN_CREATED("plan.created", "Occurs whenever a plan is created. [data.object is a plan]"),
  PLAN_DELETED("plan.deleted", "Occurs whenever a plan is deleted. [data.object is a plan]"),
  PLAN_UPDATED("plan.updated", "Occurs whenever a plan is updated. [data.object is a plan]"),
  PRICE_CREATED("price.created", "Occurs whenever a price is created. [data.object is a price]"),
  PRICE_DELETED("price.deleted", "Occurs whenever a price is deleted. [data.object is a price]"),
  PRICE_UPDATED("price.updated", "Occurs whenever a price is updated. [data.object is a price]"),
  PRODUCT_CREATED(
      "product.created", "Occurs whenever a product is created. [data.object is a product]"),
  PRODUCT_DELETED(
      "product.deleted", "Occurs whenever a product is deleted. [data.object is a product]"),
  PRODUCT_UPDATED(
      "product.updated", "Occurs whenever a product is updated. [data.object is a product]"),
  PROMOTION_CODE_CREATED(
      "promotion_code.created",
      "Occurs whenever a promotion code is created. [data.object is a promotion code]"),
  PROMOTION_CODE_UPDATED(
      "promotion_code.updated",
      "Occurs whenever a promotion code is updated. [data.object is a promotion code]"),
  QUOTE_ACCEPTED("quote.accepted", "Occurs whenever a quote is accepted. [data.object is a quote]"),
  QUOTE_CANCELED("quote.canceled", "Occurs whenever a quote is canceled. [data.object is a quote]"),
  QUOTE_CREATED("quote.created", "Occurs whenever a quote is created. [data.object is a quote]"),
  QUOTE_FINALIZED(
      "quote.finalized", "Occurs whenever a quote is finalized. [data.object is a quote]"),
  RADAR_EARLY_FRAUD_WARNING_CREATED(
      "radar.early_fraud_warning.created",
      "Occurs whenever an early fraud warning is created. [data.object is an early fraud warning]"),
  RADAR_EARLY_FRAUD_WARNING_UPDATED(
      "radar.early_fraud_warning.updated",
      "Occurs whenever an early fraud warning is updated. [data.object is an early fraud warning]"),
  RECEIVED_CREDIT_CREATED(
      "received_credit.created",
      "Occurs whenever a received_credit is created as a result of funds being pushed by another "
          + "account. [data.object is a received credit]"),
  RECEIVED_DEBIT_CREATED(
      "received_debit.created",
      "Occurs whenever a received_debit is created as a result of funds being pulled by another account. "
          + "[data.object is a received debit]"),
  RECEIVED_HOLD_AMOUNT_ADJUSTED(
      "received_hold.amount_adjusted",
      "Sent when the amount of a received_hold is adjusted. The details of the adjustment can be "
          + "found at the beginning of the adjustments_history array. [data.object is a received hold]"),
  RECEIVED_HOLD_CLOSED(
      "received_hold.closed",
      "Occurs whenever a received_hold transitions to an closed state. [data.object is a received hold]"),
  RECEIVED_HOLD_OPENED(
      "received_hold.opened",
      "Occurs whenever a received_hold transitions to an open state. [data.object is a received hold]"),
  RECEIVED_HOLD_PENDING_APPROVAL(
      "received_hold.pending_approval",
      "Sent when a received_hold requires an explicit approval or decline action. You must create "
          + "a webhook endpoint which explicitly subscribes to this event type to access it. Webhook "
          + "endpoints which subscribe to all events will not include this event type. [data.object is a "
          + "received hold]"),
  RECIPIENT_CREATED(
      "recipient.created", "Occurs whenever a recipient is created. [data.object is a recipient]"),
  RECIPIENT_DELETED(
      "recipient.deleted", "Occurs whenever a recipient is deleted. [data.object is a recipient]"),
  RECIPIENT_UPDATED(
      "recipient.updated", "Occurs whenever a recipient is updated. [data.object is a recipient]"),
  REPORTING_REPORT_RUN_FAILED(
      "reporting.report_run.failed",
      "Occurs whenever a requested ReportRun failed to complete. [data.object is a report run]"),
  REPORTING_REPORT_RUN_SUCCEEDED(
      "reporting.report_run.succeeded",
      "Occurs whenever a requested ReportRun completed succesfully. [data.object is a report run]"),
  REPORTING_REPORT_TYPE_UPDATED(
      "reporting.report_type.updated",
      "Occurs whenever a ReportType is updated (typically to indicate that a new day's data has "
          + "come available). You must create a webhook endpoint which explicitly subscribes to this "
          + "event type to access it. Webhook endpoints which subscribe to all events will not include "
          + "this event type. [data.object is a report type]"),
  REVIEW_CLOSED(
      "review.closed",
      "Occurs whenever a review is closed. The review's reason field indicates why: approved, "
          + "disputed, refunded, or refunded_as_fraud. [data.object is a review]"),
  REVIEW_OPENED("review.opened", "Occurs whenever a review is opened. [data.object is a review]"),
  SETUP_INTENT_CANCELED(
      "setup_intent.canceled",
      "Occurs when a SetupIntent is canceled. [data.object is a setup intent]"),
  SETUP_INTENT_CREATED(
      "setup_intent.created",
      "Occurs when a new SetupIntent is created. [data.object is a setup intent]"),
  SETUP_INTENT_REQUIRES_ACTION(
      "setup_intent.requires_action",
      "Occurs when a SetupIntent is in requires_action state. [data.object is a setup intent]"),
  SETUP_INTENT_SETUP_FAILED(
      "setup_intent.setup_failed",
      "Occurs when a SetupIntent has failed the attempt to setup a payment method. [data.object is a setup intent]"),
  SETUP_INTENT_SUCCEEDED(
      "setup_intent.succeeded",
      "Occurs when an SetupIntent has successfully setup a payment method. [data.object is a setup intent]"),
  SIGMA_SCHEDULED_QUERY_RUN_CREATED(
      "sigma.scheduled_query_run.created",
      "Occurs whenever a Sigma scheduled query run finishes. [data.object is a scheduled query run]"),
  SKU_CREATED("sku.created", "Occurs whenever a SKU is created. [data.object is a sku]"),
  SKU_DELETED("sku.deleted", "Occurs whenever a SKU is deleted. [data.object is a sku]"),
  SKU_UPDATED("sku.updated", "Occurs whenever a SKU is updated. [data.object is a sku]"),
  SOURCE_CANCELED(
      "source.canceled",
      "Occurs whenever a source is canceled. [data.object is a source (e.g., card)]"),
  SOURCE_CHARGEABLE(
      "source.chargeable",
      "Occurs whenever a source transitions to chargeable. [data.object is a source (e.g., card)]"),
  SOURCE_FAILED(
      "source.failed", "Occurs whenever a source fails. [data.object is a source (e.g., card)]"),
  SOURCE_MANDATE_NOTIFICATION(
      "source.mandate_notification",
      "Occurs whenever a source mandate notification method is set to manual. [data.object is a "
          + "source (e.g., card)]"),
  SOURCE_REFUND_ATTRIBUTES_REQUIRED(
      "source.refund_attributes_required",
      "Occurs whenever the refund attributes are required on a receiver source to process a refund "
          + "or a mispayment. [data.object is a source (e.g., card)]"),
  SOURCE_TRANSACTION_CREATED(
      "source.transaction.created",
      "Occurs whenever a source transaction is created. [data.object is a source transaction]"),
  SOURCE_TRANSACTION_UPDATED(
      "source.transaction.updated",
      "Occurs whenever a source transaction is updated. [data.object is a source transaction]"),
  SUBSCRIPTION_SCHEDULE_ABORTED(
      "subscription_schedule.aborted",
      "Occurs whenever a subscription schedule is canceled due to the underlying subscription "
          + "being canceled because of delinquency. [data.object is a subscription schedule]"),
  SUBSCRIPTION_SCHEDULE_CANCELED(
      "subscription_schedule.canceled",
      "Occurs whenever a subscription schedule is canceled. [data.object is a subscription schedule]"),
  SUBSCRIPTION_SCHEDULE_COMPLETED(
      "subscription_schedule.completed",
      "Occurs whenever a new subscription schedule is completed. [data.object is a subscription schedule]"),
  SUBSCRIPTION_SCHEDULE_CREATED(
      "subscription_schedule.created",
      "Occurs whenever a new subscription schedule is created. [data.object is a subscription schedule]"),
  SUBSCRIPTION_SCHEDULE_EXPIRING(
      "subscription_schedule.expiring",
      "Occurs 7 days before a subscription schedule will expire. [data.object is a subscription schedule]"),
  SUBSCRIPTION_SCHEDULE_RELEASED(
      "subscription_schedule.released",
      "Occurs whenever a new subscription schedule is released. [data.object is a subscription schedule]"),
  SUBSCRIPTION_SCHEDULE_UPDATED(
      "subscription_schedule.updated",
      "Occurs whenever a subscription schedule is updated. [data.object is a subscription schedule]"),
  TAX_RATE_CREATED(
      "tax_rate.created", "Occurs whenever a new tax rate is created. [data.object is a tax rate]"),
  TAX_RATE_UPDATED(
      "tax_rate.updated", "Occurs whenever a tax rate is updated. [data.object is a tax rate]"),
  TOPUP_CANCELED(
      "topup.canceled", "Occurs whenever a top-up is canceled. [data.object is a topup]"),
  TOPUP_CREATED("topup.created", "Occurs whenever a top-up is created. [data.object is a topup]"),
  TOPUP_FAILED("topup.failed", "Occurs whenever a top-up fails. [data.object is a topup]"),
  TOPUP_REVERSED(
      "topup.reversed", "Occurs whenever a top-up is reversed. [data.object is a topup]"),
  TOPUP_SUCCEEDED("topup.succeeded", "Occurs whenever a top-up succeeds. [data.object is a topup]"),
  TRANSFER_CREATED(
      "transfer.created", "Occurs whenever a transfer is created. [data.object is a transfer]"),
  TRANSFER_FAILED(
      "transfer.failed", "Occurs whenever a transfer failed. [data.object is a transfer]"),
  TRANSFER_PAID(
      "transfer.paid",
      "Occurs after a transfer is paid. For Instant Payouts, the event will typically be sent "
          + "within 30 minutes. [data.object is a transfer]"),
  TRANSFER_REVERSED(
      "transfer.reversed",
      "Occurs whenever a transfer is reversed, including partial reversals. [data.object is a transfer]"),
  TRANSFER_UPDATED(
      "transfer.updated",
      "Occurs whenever a transfer's description or metadata is updated. [data.object is a transfer]"),
  ;

  private String stripeEventType;
  private String description;

  private static Map<String, StripeEventType> map = initializeMap();

  private static Map<String, StripeEventType> initializeMap() {
    return Arrays.stream(StripeEventType.values())
        .collect(Collectors.toUnmodifiableMap(e -> e.stripeEventType, Function.identity()));
  }

  StripeEventType(String stripeEventType, String description) {
    this.stripeEventType = stripeEventType;
    this.description = description;
  }

  static StripeEventType fromString(String stripeEventType) {
    StripeEventType eventType = map.get(stripeEventType);
    return eventType != null ? eventType : UNKNOWN;
  }
}
