package com.clearspend.capital.client.i2c.push.controller.type;

import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class I2cCard {

  @ApiModelProperty(
      value =
          "Card Account Number This tag will only be available for PCI certified clients. "
              + "Type: N 19")
  @JsonProperty("CardNo")
  private String cardNumber;

  // field to capture the card number in an encrypted format when writing to the database
  // TODO(kuchlein): See if this causes us any problems, if not apply this to all other PII fields
  private NullableEncryptedString encryptedCardNumber;

  @ApiModelProperty(value = "ID of the Card Program. " + "Type: Str 19")
  @JsonProperty("CardProgramID")
  private String cardProgramRef;

  @ApiModelProperty(
      value = "Reference Number which uniquely identifies this Card Account. " + "Type: N 15")
  @JsonProperty("CardReferenceID")
  private String cardReferenceRef;

  @ApiModelProperty(
      value =
          "Number of the Primary Account against which this Card is Issue. "
              + "This tag will only be available for PCI certified clients. "
              + "Type: N 19")
  @JsonProperty("PrimaryCardNo")
  private String primaryCardNumber;

  @ApiModelProperty(
      value = "Reference Number which uniquely identifies a (primary) card. " + "Type: N 15")
  @JsonProperty("PrimaryCardReferenceID")
  private String primaryCardReferenceRef;

  @ApiModelProperty(
      value = "Cardholder ID to whom Card Account is Issue/Registered. " + "Type: Str 20")
  @JsonProperty("CustomerId")
  private String customerRef;

  @ApiModelProperty(value = "Cardholder's Member ID. " + "Type: Str 25")
  @JsonProperty("MemberId")
  private String memberRef;

  @ApiModelProperty(
      value =
          "Card Account Balance. If the available balance on Card Account is "
              + "negative it will be represented with -ve sign (i.e., -8.60). "
              + "Type: N 10, 2")
  @JsonProperty("AvailableBalance")
  private String availableBalance;

  @ApiModelProperty(
      value =
          "Card Account Ledger Balance If the ledger balance on Card Account is "
              + "negative it will be represented with -ve sign (i.e., -8.60). "
              + "Type: N 10, 2")
  @JsonProperty("LedgerBalance")
  private String ledgerBalance;

  @ApiModelProperty(
      value =
          "Status Code of Card Account. Refer to the Appendix B: Card Status Codes. "
              + "Type: char")
  @JsonProperty("CardStatus")
  private String cardStatus;

  @ApiModelProperty(value = "Cardholder's First Name. " + "Type: Str 26")
  @JsonProperty("FirstName")
  private String firstName;

  @ApiModelProperty(value = "Cardholder's Last Name. " + "Type: Str 26")
  @JsonProperty("LastName")
  private String lastName;

  @ApiModelProperty(value = "Cardholder's Address (Line 1). " + "Type: Str 30")
  @JsonProperty("AddressLine1")
  private String addressLine1;

  @ApiModelProperty(value = "Cardholder's Address (Line 2). " + "Type: Str 30")
  @JsonProperty("AddressLine2")
  private String addressLine2;

  @ApiModelProperty(value = "Cardholder's City. " + "Type: AN 40")
  @JsonProperty("City")
  private String locality;

  @ApiModelProperty(value = "Cardholder's State. " + "Type: AN 15")
  @JsonProperty("State")
  private String region;

  @ApiModelProperty(value = "Cardholder's Postal/Zip Code. " + "Type: Str 20")
  @JsonProperty("PostalCode")
  private String postalCode;

  @ApiModelProperty(value = "Cardholder's Country Code. " + "Type: AN 3")
  @JsonProperty("CountryCode")
  private String country;

  @ApiModelProperty(value = "Cardholder's Cell Phone No. " + "Type: N 15")
  @JsonProperty("CellNo")
  private String phone;

  @ApiModelProperty(value = "Cardholder's E-mail address. " + "Type: Str 100")
  @JsonProperty("Email")
  private String email;

  @ApiModelProperty(value = "Source card reference number. " + "Type: AN 40")
  @JsonProperty("SourceCardReferenceNo")
  private String sourceCardReferenceNumber;

  @ApiModelProperty(value = "Source card number. " + "Type: N 19")
  @JsonProperty("SourceCardNo")
  private String sourceCardNumber;
}
