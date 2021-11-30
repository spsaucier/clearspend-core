package com.tranwall.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.OffsetTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class I2cTransaction {

  @ApiModelProperty(value = "Unique ID assigned to the Push Notification Request. " + "Type: N 40")
  @JsonProperty("NotificationEventId")
  private String notificationEventRef;

  @ApiModelProperty(value = "Transaction Identifier. " + "Type: N 11")
  @JsonProperty("TransactionId")
  private String transactionRef; // on common

  @ApiModelProperty(
      value =
          "Message Type Identifier "
              + "Refer to Appendix ISO Message Types to see the possible values for this field")
  @JsonProperty("MessageType")
  private String messageType; // on common

  @ApiModelProperty(value = "Date of the Transaction Date Format: YYYY-MM-DD")
  @JsonProperty("Date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate date; // on common

  @ApiModelProperty(value = "Time of Transaction Date Format: HH:MM:SS")
  @JsonProperty("Time")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
  private OffsetTime time; // on common

  @JsonProperty("CardAcceptor")
  private CardAcceptor cardAcceptor;

  @ApiModelProperty(
      value =
          "Transaction Type Identifier. "
              + "Refer to Appendix Transaction Type Codes to see the possible values for this field. "
              + "Type: AN2")
  @JsonProperty("TransactionType")
  private String transactionType;

  @ApiModelProperty(value = "Service applied on Transaction. " + "Type: Str 120")
  @JsonProperty("Service")
  private String service;

  @ApiModelProperty(value = "The transaction amount requested by the merchant")
  @JsonProperty("RequestedAmount")
  private String requestedAmount; // on common

  @ApiModelProperty(value = "Currency of the Requested Amount")
  @JsonProperty("RequestedAmountCurrency")
  private String requestedAmountCurrency; // on common

  @ApiModelProperty(
      value =
          "Amount posted on Card Account "
              + "If the amount is to be debited from the Card Account then it will be represented "
              + "with -ve sign (i.e., -100.00)")
  @JsonProperty("TransactionAmount")
  private String transactionAmount; // on common

  @ApiModelProperty(value = "ISO Currency Code of Card Account")
  @JsonProperty("TransactionCurrency")
  private String transactionCurrency; // on common

  @ApiModelProperty(
      value =
          "Status of Transaction "
              + "Refer to Appendix Transaction Response Codes to see the possible values for this "
              + "field. "
              + "Type: AN 2")
  @JsonProperty("TransactionResponseCode")
  private String transactionResponseCode;

  @ApiModelProperty(value = "The interchange fee charged from the merchant. " + "Type: N 10, 2")
  @JsonProperty("InterchangeFee")
  private String interchangeFee;

  @ApiModelProperty(
      value =
          "Indicated how the PAN was captured. Please refer to the network manual (see: "
              + "https://en.wikipedia.org/wiki/ISO_8583#:~:text=for%20private%20use-,Point%20of%20service%20entry%20modes,-%5Bedit%5D). "
              + "Type: AN 3")
  @JsonProperty("PANEntryMode")
  private String panEntryMode;

  @ApiModelProperty(
      value =
          "For network transactions, it contains the authorization code. "
              + "For ACH transactions, it contains the Return Code (for ACH returns) or the "
              + "Notification Of Change (NOC) Code (in case a NOC is received). "
              + "Type: AN 16")
  @JsonProperty("AuthorizationCode")
  private String authorizationCode;

  @ApiModelProperty(
      value =
          "Acquirer reference number. "
              + "This is a unique reference provided by the acquirer in the call. "
              + "Type: AN 20")
  @JsonProperty("ARN")
  private String acquirerReferenceNumber;

  @ApiModelProperty(
      value = "A unique transaction reference provided by the acquirer. " + "Type: AN 80")
  @JsonProperty("RetrievalReferenceNo")
  private String retrievalReferenceNumber;

  @ApiModelProperty(
      value = "A unique transaction reference provided by the network. " + "Type: AN 24")
  @JsonProperty("SystemTraceAuditNo")
  private String systemTraceAuditNumber;

  @ApiModelProperty(value = "Identifies the network. " + "Type: AN 25")
  @JsonProperty("NetworkId")
  private String networkRef;

  @ApiModelProperty(
      value =
          "Identifier of the Original Transaction against which this Transaction is Posted. "
              + "Type: N 11")
  @JsonProperty("OriginalTransId")
  private String originalTransactionRef;

  @ApiModelProperty(
      value =
          "A unique i2c identifier for ACH transactions. "
              + "Used to uniquely identify Bank to Card and Card to Bank Transfers in the system. "
              + "Type: N 30")
  @JsonProperty("TransferID")
  private String transferRef;

  @ApiModelProperty(
      value =
          "A unique i2c internal identifier for a bank account definition in "
              + "our system. Used in ACH transfers. "
              + "Type: AN 31")
  @JsonProperty("BankAccountNumber")
  private String bankAccountNumber;

  @ApiModelProperty(
      value =
          "A user friendly description, explaining the purpose of the "
              + "transaction. "
              + "Type: Str 255")
  @JsonProperty("TransactionDescription")
  private String transactionDescription;

  @ApiModelProperty(value = "Reserved for i2c use. " + "Type Str 40")
  @JsonProperty("ExternalTransReference")
  private String externalTransReference;

  @ApiModelProperty(value = "Reserved for i2c use. " + "Type Str 40")
  @JsonProperty("ExternalUserReference")
  private String externalUserReference;

  @ApiModelProperty(value = "Reference number of the linked card account. " + "Type: AN 11")
  @JsonProperty("ExternalLinkedCardRefID")
  private String externalLinkedCardRefRef;

  @ApiModelProperty(value = "Profile information of the linked card account. " + "Type: Str 255")
  @JsonProperty("ExternalLinkedCardProfileSet1")
  private String externalLinkedCardProfileSet1;

  @ApiModelProperty(value = "Profile information of the linked card account. " + "Type: Str 255")
  @JsonProperty("ExternalLinkedCardProfileSet2")
  private String externalLinkedCardProfileSet2;

  @ApiModelProperty(value = "The Primary Account Number sequence number. " + "Type: AN 3")
  @JsonProperty("PanSequenceNo")
  private String panSequenceNumber;

  @ApiModelProperty(value = "Identifies the invoked fraud parameter. " + "Type: AN 20")
  @JsonProperty("FraudParameter")
  private String fraudParameter;
}
