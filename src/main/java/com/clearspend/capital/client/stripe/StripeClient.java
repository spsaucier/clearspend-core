package com.clearspend.capital.client.stripe;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.User;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class StripeClient {

  private final StripeProperties stripeProperties;
  private final ObjectMapper objectMapper;

  private interface StripeProducer<T extends ApiResource> {
    T produce() throws StripeException;
  }

  public Account createAccount(Business business) {
    Map<String, Object> requestedCapability = Map.of("requested", "true");

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
            .setAddress(
                Address.builder()
                    .setCity(business.getClearAddress().getLocality())
                    .setCountry(business.getClearAddress().getCountry().getTwoCharacterCode())
                    .setLine1(business.getClearAddress().getStreetLine1())
                    .setLine2(business.getClearAddress().getStreetLine2())
                    .setPostalCode(business.getClearAddress().getPostalCode())
                    .build())
            .build();

    Capabilities capabilities =
        Capabilities.builder()
            .setTransfers(Transfers.builder().setRequested(true).build())
            .setCardPayments(CardPayments.builder().setRequested(true).build())
            .setCardIssuing(CardIssuing.builder().setRequested(true).build())
            .putExtraParam("treasury", requestedCapability)
            .putExtraParam("us_bank_account_ach_payments", requestedCapability)
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

    return callStripe(
        "createAccount",
        accountCreateParams,
        () -> Account.create(accountCreateParams, getRequestOptions(business.getId())));
  }

  public Cardholder createCardholder(User user) {
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
                CardholderCreateParams.Billing.builder()
                    .setAddress(
                        CardholderCreateParams.Billing.Address.builder()
                            .setLine1(user.getAddress().getStreetLine1().getEncrypted())
                            .setLine2(user.getAddress().getStreetLine2().getEncrypted())
                            .setCity(user.getAddress().getLocality())
                            .setState(user.getAddress().getRegion())
                            .setPostalCode(user.getAddress().getPostalCode().getEncrypted())
                            .setCountry(user.getAddress().getCountry().getTwoCharacterCode())
                            .build())
                    .build())
            .build();

    return callStripe(
        "createCardholder",
        params,
        () -> Cardholder.create(params, getRequestOptions(user.getId())));
  }

  public Person createPerson(BusinessOwner businessOwner, String businessExternalRef) {
    Builder builder =
        PersonCollectionCreateParams.builder()
            .setFirstName(businessOwner.getFirstName().getEncrypted())
            .setLastName(businessOwner.getLastName().getEncrypted())
            .setEmail(businessOwner.getEmail().getEncrypted())
            .setPhone(businessOwner.getPhone().getEncrypted())
            .setRelationship(Relationship.builder().setRepresentative(true).setOwner(true).build())
            .setAddress(
                PersonCollectionCreateParams.Address.builder()
                    .setLine1(businessOwner.getAddress().getStreetLine1().getEncrypted())
                    .setLine2(businessOwner.getAddress().getStreetLine2().getEncrypted())
                    .setCity(businessOwner.getAddress().getLocality())
                    .setState(businessOwner.getAddress().getRegion())
                    .setPostalCode(businessOwner.getAddress().getPostalCode().getEncrypted())
                    .setCountry("US")
                    .build());

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
                    .setAddress(
                        Shipping.Address.builder()
                            .setLine1(shippingAddress.getStreetLine1().getEncrypted())
                            .setLine2(shippingAddress.getStreetLine2().getEncrypted())
                            .setCity(shippingAddress.getLocality())
                            .setState(shippingAddress.getRegion())
                            .setPostalCode(shippingAddress.getPostalCode().getEncrypted())
                            .setCountry("US")
                            .build())
                    .build())
            .build();

    return callStripe(
        "createCard",
        cardParameters,
        () ->
            com.stripe.model.issuing.Card.create(cardParameters, getRequestOptions(card.getId())));
  }

  private <T extends ApiResource> T callStripe(
      String methodName, ApiRequestParams params, StripeProducer<T> function) {
    T result;

    try {
      result = function.produce();
      log.info(
          "Calling stripe [%s] method. \n Request: %s, \n Response: %s"
              .formatted(methodName, objectMapper.writeValueAsString(params), result));
    } catch (StripeException e) {
      throw new StripeClientException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert java object to json", e);
    }

    return result;
  }

  // see https://stripe.com/docs/api/idempotent_requests
  private RequestOptions getRequestOptions(TypedId<?> idempotencyKey) {
    return RequestOptions.builder().setIdempotencyKey(idempotencyKey.toString()).build();
  }
}
