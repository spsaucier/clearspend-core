package com.clearspend.capital.client.stripe;

import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Person;
import com.stripe.model.PersonCollection;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Cardholder;
import com.stripe.net.ApiRequestParams;
import com.stripe.net.ApiResource;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountCreateParams.BusinessType;
import com.stripe.param.AccountCreateParams.Capabilities;
import com.stripe.param.AccountCreateParams.Capabilities.CardIssuing;
import com.stripe.param.AccountCreateParams.Capabilities.CardPayments;
import com.stripe.param.AccountCreateParams.Capabilities.Transfers;
import com.stripe.param.AccountCreateParams.Company;
import com.stripe.param.AccountCreateParams.Company.Address;
import com.stripe.param.AccountCreateParams.TosAcceptance;
import com.stripe.param.AccountCreateParams.Type;
import com.stripe.param.PersonCollectionCreateParams;
import com.stripe.param.PersonCollectionCreateParams.Builder;
import com.stripe.param.PersonCollectionCreateParams.Dob;
import com.stripe.param.PersonCollectionCreateParams.Relationship;
import com.stripe.param.issuing.CardCreateParams;
import com.stripe.param.issuing.CardCreateParams.Shipping;
import com.stripe.param.issuing.CardCreateParams.Shipping.Service;
import com.stripe.param.issuing.CardCreateParams.Status;
import com.stripe.param.issuing.CardUpdateParams;
import com.stripe.param.issuing.CardholderCreateParams;
import com.stripe.param.issuing.CardholderCreateParams.Billing;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Profile("!test")
public class StripeClient {

  private static final Map<String, Object> REQUESTED_CAPABILITY = Map.of("requested", "true");

  private final StripeProperties stripeProperties;
  private final ObjectMapper objectMapper;
  private final WebClient stripeTreasuryWebClient;
  private final boolean testMode;

  public StripeClient(
      StripeProperties stripeProperties,
      ObjectMapper objectMapper,
      @Qualifier("stripeTreasuryWebClient") WebClient stripeTreasuryWebClient) {
    this.stripeProperties = stripeProperties;
    this.objectMapper = objectMapper;
    this.stripeTreasuryWebClient = stripeTreasuryWebClient;

    testMode = StringUtils.startsWith(stripeProperties.getApiKey(), "sk_test");
  }

  private interface StripeProducer<T extends ApiResource> {

    T produce() throws StripeException;
  }

  public Account createAccount(Business business) {
    Address.Builder addressBuilder =
        Address.builder()
            .setLine1(business.getClearAddress().getStreetLine1())
            .setPostalCode(business.getClearAddress().getPostalCode())
            .setCity(business.getClearAddress().getLocality())
            .setState(business.getClearAddress().getRegion())
            .setCountry(business.getClearAddress().getCountry().getTwoCharacterCode());
    if (StringUtils.isNotEmpty(business.getClearAddress().getStreetLine2())) {
      addressBuilder.setLine2(business.getClearAddress().getStreetLine2());
    }

    Company company =
        Company.builder()
            .setName(business.getLegalName())
            .setTaxId(business.getEmployerIdentificationNumber())
            .setPhone(business.getBusinessPhone().getEncrypted())
            // TODO: Implement proper conversion
            /*
                        .setStructure(
                            switch (business.getType()) {
                              case LLC -> Structure.LLC;
                              case LLP -> Structure.LIMITED_LIABILITY_PARTNERSHIP;
                              case SOLE_PROPRIETORSHIP -> Structure.SOLE_PROPRIETORSHIP;
                              default -> Structure.PRIVATE_COMPANY;
                            })
            */
            .setAddress(addressBuilder.build())
            .build();

    Capabilities capabilities =
        Capabilities.builder()
            .setTransfers(Transfers.builder().setRequested(true).build())
            .setCardPayments(CardPayments.builder().setRequested(true).build())
            .setCardIssuing(CardIssuing.builder().setRequested(true).build())
            .putExtraParam("treasury", REQUESTED_CAPABILITY)
            .putExtraParam("us_bank_account_ach_payments", REQUESTED_CAPABILITY)
            .build();

    AccountCreateParams accountCreateParams =
        AccountCreateParams.builder()
            .setType(Type.CUSTOM)
            .setBusinessType(BusinessType.COMPANY)
            .setCountry(business.getClearAddress().getCountry().getTwoCharacterCode())
            .setEmail(business.getBusinessEmail().getEncrypted())
            .setCompany(company)
            .setCapabilities(capabilities)
            .setTosAcceptance(
                TosAcceptance.builder()
                    // TODO: identify proper values
                    .setDate(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))
                    .setIp(stripeProperties.getTosAcceptanceIp())
                    .build())
            .build();

    Account account =
        callStripe(
            "createAccount",
            accountCreateParams,
            () -> Account.create(accountCreateParams, getRequestOptions(business.getId())));

    createFinancialAccount(business.getId(), account.getId());

    return account;
  }

  public Cardholder createCardholder(User user, ClearAddress billingAddress) {
    Billing.Address.Builder addressBuilder =
        Billing.Address.builder()
            .setLine1(billingAddress.getStreetLine1())
            .setCity(billingAddress.getLocality())
            .setState(billingAddress.getRegion())
            .setPostalCode(billingAddress.getPostalCode())
            .setCountry(billingAddress.getCountry().getTwoCharacterCode());
    if (StringUtils.isNotEmpty(billingAddress.getStreetLine2())) {
      addressBuilder.setLine2(billingAddress.getStreetLine2());
    }

    CardholderCreateParams params =
        CardholderCreateParams.builder()
            .setName(
                "%s %s"
                    .formatted(
                        user.getFirstName().getEncrypted(), user.getLastName().getEncrypted()))
            .setEmail(user.getEmail().getEncrypted())
            .setPhoneNumber(user.getPhone().getEncrypted())
            .setStatus(CardholderCreateParams.Status.ACTIVE)
            .setType(CardholderCreateParams.Type.INDIVIDUAL)
            .setBilling(
                CardholderCreateParams.Billing.builder().setAddress(addressBuilder.build()).build())
            .build();

    return callStripe(
        "createCardholder",
        params,
        () -> Cardholder.create(params, getRequestOptions(user.getId())));
  }

  public Person createPerson(BusinessOwner businessOwner, String businessExternalRef) {
    PersonCollectionCreateParams.Address.Builder addressBuilder =
        PersonCollectionCreateParams.Address.builder()
            .setLine1(businessOwner.getAddress().getStreetLine1().getEncrypted())
            .setCity(businessOwner.getAddress().getLocality())
            .setState(businessOwner.getAddress().getRegion())
            .setPostalCode(businessOwner.getAddress().getPostalCode().getEncrypted())
            .setCountry(businessOwner.getAddress().getCountry().getTwoCharacterCode());
    if (StringUtils.isNotEmpty(businessOwner.getAddress().getStreetLine1().getEncrypted())) {
      addressBuilder.setLine2(businessOwner.getAddress().getStreetLine1().getEncrypted());
    }

    Builder builder =
        PersonCollectionCreateParams.builder()
            .setFirstName(businessOwner.getFirstName().getEncrypted())
            .setLastName(businessOwner.getLastName().getEncrypted())
            .setEmail(businessOwner.getEmail().getEncrypted())
            .setPhone(businessOwner.getPhone().getEncrypted())
            .setRelationship(Relationship.builder().setRepresentative(true).setOwner(true).build())
            .setAddress(addressBuilder.build());

    if (businessOwner.getDateOfBirth() != null) {
      builder.setDob(
          Dob.builder()
              .setYear((long) businessOwner.getDateOfBirth().getYear())
              .setMonth((long) businessOwner.getDateOfBirth().getMonth().getValue())
              .setDay((long) businessOwner.getDateOfBirth().getDayOfMonth())
              .build());
    }

    PersonCollectionCreateParams personParameters = builder.build();

    // TODO: Is there a proper api way to get children collection via static methods rather than
    // passing the proper URL?
    PersonCollection personCollection = new PersonCollection();
    personCollection.setUrl("/v1/accounts/%s/persons".formatted(businessExternalRef));
    return callStripe(
        "createPerson",
        personParameters,
        () -> personCollection.create(personParameters, getRequestOptions(businessOwner.getId())));
  }

  public Card updateCard(String cardExternalRef, CardStatus cardStatus) {
    Card card = new Card();
    card.setId(cardExternalRef);

    CardUpdateParams params =
        CardUpdateParams.builder()
            .setStatus(
                switch (cardStatus) {
                  case ACTIVE -> CardUpdateParams.Status.ACTIVE;
                  case INACTIVE -> CardUpdateParams.Status.INACTIVE;
                  case CANCELLED -> CardUpdateParams.Status.CANCELED;
                })
            .build();

    return callStripe("updateCard", params, () -> card.update(params));
  }

  public Card createVirtualCard(
      com.clearspend.capital.data.model.Card card, String userExternalRef) {
    CardCreateParams cardParameters =
        CardCreateParams.builder()
            .setCardholder(userExternalRef)
            .setCurrency(Currency.USD.name())
            .setType(CardCreateParams.Type.VIRTUAL)
            .setStatus(Status.ACTIVE)
            .build();
    log.debug("Virtual card: cardParameters: {}", cardParameters);

    return callStripe(
        "createCard",
        cardParameters,
        () ->
            com.stripe.model.issuing.Card.create(cardParameters, getRequestOptions(card.getId())));
  }

  public Card createPhysicalCard(
      com.clearspend.capital.data.model.Card card,
      com.clearspend.capital.common.data.model.Address shippingAddress,
      String userExternalRef) {
    Shipping.Address.Builder addressBuilder =
        Shipping.Address.builder()
            .setLine1(shippingAddress.getStreetLine1().getEncrypted())
            .setCity(shippingAddress.getLocality())
            .setState(shippingAddress.getRegion())
            .setPostalCode(shippingAddress.getPostalCode().getEncrypted())
            .setCountry(shippingAddress.getCountry().getTwoCharacterCode());
    if (StringUtils.isNotEmpty(shippingAddress.getStreetLine2().getEncrypted())) {
      addressBuilder.setLine2(shippingAddress.getStreetLine2().getEncrypted());
    }

    CardCreateParams cardParameters =
        CardCreateParams.builder()
            .setCardholder(userExternalRef)
            .setCurrency(Currency.USD.name())
            .setType(CardCreateParams.Type.PHYSICAL)
            .setStatus(Status.INACTIVE)
            .setShipping(
                Shipping.builder()
                    // TODO: Should be a part of the user request when issuing a card
                    .setName("Some funny looking shipping label")
                    .setService(Service.STANDARD)
                    .setAddress(addressBuilder.build())
                    .build())
            .build();
    log.debug("Physical card: cardParameters: {}", cardParameters);

    return callStripe(
        "createCard",
        cardParameters,
        () ->
            com.stripe.model.issuing.Card.create(cardParameters, getRequestOptions(card.getId())));
  }

  public FinancialAccount createFinancialAccount(
      TypedId<BusinessId> businessId, String accountExternalRef) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("supported_currencies[]", "usd");
    formData.add("features[card_issuing][requested]", "true");
    formData.add("features[deposit_insurance][requested]", "true");
    formData.add("features[financial_addresses][aba][requested]", "true");
    formData.add("features[inbound_transfers][ach][requested]", "true");
    formData.add("features[intra_stripe_flows][requested]", "true");
    formData.add("features[outbound_payments][ach][requested]", "true");
    formData.add("features[outbound_payments][us_domestic_wire][requested]", "true");
    formData.add("features[outbound_transfers][ach][requested]", "true");
    formData.add("features[outbound_transfers][us_domestic_wire][requested]", "true");
    if (testMode) {
      formData.add("testmode_bypass_requirements", "true");
    }

    FinancialAccount financialAccount =
        stripeTreasuryWebClient
            .post()
            .uri("/financial_accounts")
            .headers(
                httpHeaders -> {
                  httpHeaders.add("Stripe-Account", accountExternalRef);
                  httpHeaders.add("Idempotency-Key", "fa_" + businessId);
                })
            .body(BodyInserters.fromFormData(formData))
            .exchangeToMono(
                response -> {
                  if (response.statusCode().equals(HttpStatus.OK)) {
                    return response.bodyToMono(FinancialAccount.class);
                  } else {
                    return response.createException().flatMap(Mono::error);
                  }
                })
            .block();

    log.info(
        "Calling stripe createFinancialAccount method. \n Response: %s"
            .formatted(financialAccount));

    return financialAccount;
  }

  private <T extends ApiResource> T callStripe(
      String methodName, ApiRequestParams params, StripeProducer<T> function) {
    T result = null;
    String request = null;

    try {
      request = objectMapper.writeValueAsString(params);
      result = function.produce();
    } catch (StripeException e) {
      throw new StripeClientException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert java object to json", e);
    } finally {
      log.info(
          "Calling stripe [%s] method. \n Request: %s, \n Response: %s"
              .formatted(methodName, request, result));
    }

    return result;
  }

  // see https://stripe.com/docs/api/idempotent_requests
  private RequestOptions getRequestOptions(TypedId<?> idempotencyKey) {
    return RequestOptions.builder().setIdempotencyKey(idempotencyKey.toString()).build();
  }

  /**
   * Returns an ephemeral key which frontend can use to reveal the Virtual card details in the
   * PCI-compliant way
   */
  public String getEphemeralKey(String cardId, String nonce) {
    Map<String, Object> params = new HashMap<>();
    params.put("issuing_card", cardId);
    params.put("nonce", nonce);
    try {
      var res =
          com.stripe.model.EphemeralKey.create(
              params,
              RequestOptions.builder()
                  .setIdempotencyKey(cardId + nonce)
                  .setStripeVersionOverride("2020-03-02")
                  .build());
      return res.getSecret();
    } catch (StripeException e) {
      throw new StripeClientException(e);
    }
  }
}
