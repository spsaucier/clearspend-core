package com.clearspend.capital.client.stripe;

import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundPayment;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.error.StripeAccountDocumentUpdateException;
import com.clearspend.capital.common.error.StripePersonDocumentUpdateException;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.EphemeralKey;
import com.stripe.model.File;
import com.stripe.model.Person;
import com.stripe.model.PersonCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Cardholder;
import com.stripe.net.ApiRequestParams;
import com.stripe.net.ApiResource;
import com.stripe.net.RequestOptions;
import com.stripe.net.RequestOptions.RequestOptionsBuilder;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountCreateParams.BusinessProfile;
import com.stripe.param.AccountCreateParams.Capabilities;
import com.stripe.param.AccountCreateParams.Capabilities.CardIssuing;
import com.stripe.param.AccountCreateParams.Capabilities.CardPayments;
import com.stripe.param.AccountCreateParams.Capabilities.Transfers;
import com.stripe.param.AccountCreateParams.Company;
import com.stripe.param.AccountCreateParams.Company.Address;
import com.stripe.param.AccountCreateParams.Settings;
import com.stripe.param.AccountCreateParams.TosAcceptance;
import com.stripe.param.AccountCreateParams.Type;
import com.stripe.param.AccountUpdateParams;
import com.stripe.param.AccountUpdateParams.Company.Structure;
import com.stripe.param.FileCreateParams;
import com.stripe.param.FileCreateParams.Purpose;
import com.stripe.param.PersonCollectionCreateParams;
import com.stripe.param.PersonCollectionCreateParams.Builder;
import com.stripe.param.PersonCollectionCreateParams.Dob;
import com.stripe.param.PersonCollectionCreateParams.Relationship;
import com.stripe.param.PersonUpdateParams;
import com.stripe.param.PersonUpdateParams.Verification;
import com.stripe.param.PersonUpdateParams.Verification.Document;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams.MandateData;
import com.stripe.param.SetupIntentCreateParams.MandateData.CustomerAcceptance;
import com.stripe.param.issuing.AuthorizationApproveParams;
import com.stripe.param.issuing.AuthorizationDeclineParams;
import com.stripe.param.issuing.CardCreateParams;
import com.stripe.param.issuing.CardCreateParams.Shipping;
import com.stripe.param.issuing.CardCreateParams.Shipping.Service;
import com.stripe.param.issuing.CardCreateParams.SpendingControls;
import com.stripe.param.issuing.CardCreateParams.SpendingControls.SpendingLimit;
import com.stripe.param.issuing.CardCreateParams.SpendingControls.SpendingLimit.Interval;
import com.stripe.param.issuing.CardCreateParams.Status;
import com.stripe.param.issuing.CardUpdateParams;
import com.stripe.param.issuing.CardholderCreateParams;
import com.stripe.param.issuing.CardholderCreateParams.Billing;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Profile("!test")
public class StripeClient {

  private static final Map<String, Object> REQUESTED_CAPABILITY = Map.of("requested", "true");

  // Treasury related API calls requires Stripe-treasury beta capabilities,
  // which can be enabled using a special version string
  // passed in 'Stripe-Version' header or in 'StripeVersionOverride' parameter
  // Assumption is that in 03/2022, once the GA version will be available, we will remove or update
  // this indicator further
  private static final String STRIPE_BETA_HEADER =
      "2020-08-27;treasury_beta=v1;financial_accounts_beta=v3;money_flows_beta=v3;transactions_beta=v3;us_bank_account_beta=v2;issuing_flows_beta=v1";

  private final StripeProperties stripeProperties;
  private final ObjectMapper objectMapper;
  private final WebClient stripeTreasuryWebClient;

  public StripeClient(
      StripeProperties stripeProperties,
      ObjectMapper objectMapper,
      @Qualifier("stripeTreasuryWebClient") WebClient stripeTreasuryWebClient) {
    this.stripeProperties = stripeProperties;
    this.objectMapper = objectMapper;
    this.stripeTreasuryWebClient = stripeTreasuryWebClient;
  }

  private interface StripeProducer<T extends ApiResource> {

    T produce() throws StripeException;
  }

  public Account createAccount(Business business) {

    Company.Builder companyBuilder =
        Company.builder()
            .setName(business.getLegalName())
            .setTaxId(business.getEmployerIdentificationNumber())
            .setStructure(business.getType().getStripeValue());

    AccountCreateParams.Builder accountBuilder =
        AccountCreateParams.builder()
            .setType(Type.CUSTOM)
            .setBusinessType(business.getType().getStripeBusinessType())
            .setBusinessProfile(
                BusinessProfile.builder()
                    .setName(business.getBusinessName())
                    .setMcc(business.getMcc())
                    .setProductDescription(business.getDescription())
                    .setUrl(business.getUrl())
                    .build());

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

    companyBuilder
        .setPhone(business.getBusinessPhone().getEncrypted())
        .setAddress(addressBuilder.build());

    accountBuilder.setEmail(business.getBusinessEmail().getEncrypted());

    accountBuilder.setCountry(business.getClearAddress().getCountry().getTwoCharacterCode());

    accountBuilder.setCompany(companyBuilder.build());
    accountBuilder.setCapabilities(
        Capabilities.builder()
            .setTransfers(Transfers.builder().setRequested(true).build())
            .setCardPayments(CardPayments.builder().setRequested(true).build())
            .setCardIssuing(CardIssuing.builder().setRequested(true).build())
            .putExtraParam("treasury", REQUESTED_CAPABILITY)
            .putExtraParam("us_bank_account_ach_payments", REQUESTED_CAPABILITY)
            .build());

    AccountCreateParams accountCreateParams =
        accountBuilder
            .setTosAcceptance(
                TosAcceptance.builder()
                    .setDate(
                        business
                            .getStripeData()
                            .getTosAcceptance()
                            .getDate()
                            .toInstant()
                            .getEpochSecond())
                    .setIp(business.getStripeData().getTosAcceptance().getIp())
                    .setUserAgent(business.getStripeData().getTosAcceptance().getUserAgent())
                    .setServiceAgreement("full")
                    .build())
            .setSettings(
                Settings.builder()
                    .setCardIssuing(
                        Settings.CardIssuing.builder()
                            .setTosAcceptance(
                                Settings.CardIssuing.TosAcceptance.builder()
                                    .setDate(
                                        business
                                            .getStripeData()
                                            .getTosAcceptance()
                                            .getDate()
                                            .toInstant()
                                            .getEpochSecond())
                                    .setIp(business.getStripeData().getTosAcceptance().getIp())
                                    .setUserAgent(
                                        business.getStripeData().getTosAcceptance().getUserAgent())
                                    .build())
                            .build())
                    .putExtraParam(
                        "treasury",
                        Map.of(
                            "tos_acceptance",
                            Map.of(
                                "date",
                                business
                                    .getStripeData()
                                    .getTosAcceptance()
                                    .getDate()
                                    .toInstant()
                                    .getEpochSecond(),
                                "ip",
                                business.getStripeData().getTosAcceptance().getIp(),
                                "user_agent",
                                business.getStripeData().getTosAcceptance().getUserAgent())))
                    .build())
            .build();

    return callStripe(
        "createAccount",
        accountCreateParams,
        () -> Account.create(accountCreateParams, getRequestOptionsBetaApi(new TypedId<>(), null)));
  }

  public Account updateAccount(Business business) {

    Account account = new Account();
    String stripeAccountId = business.getStripeData().getAccountRef();
    account.setId(stripeAccountId);

    AccountUpdateParams.Company.Builder companyBuilder =
        AccountUpdateParams.Company.builder()
            .setName(business.getLegalName())
            .setTaxId(business.getEmployerIdentificationNumber())
            .setStructure(Structure.valueOf(business.getType().getStripeValue().name()));

    AccountUpdateParams.Builder accountBuilder =
        AccountUpdateParams.builder()
            .setBusinessType(business.getType().getStripeBusinessType().getValue())
            .setBusinessProfile(
                AccountUpdateParams.BusinessProfile.builder()
                    .setName(business.getBusinessName())
                    .setMcc(business.getMcc())
                    .setProductDescription(business.getDescription())
                    .setUrl(business.getUrl())
                    .build());

    AccountUpdateParams.Company.Address.Builder addressBuilder =
        AccountUpdateParams.Company.Address.builder()
            .setLine1(business.getClearAddress().getStreetLine1())
            .setPostalCode(business.getClearAddress().getPostalCode())
            .setCity(business.getClearAddress().getLocality())
            .setState(business.getClearAddress().getRegion())
            .setCountry(business.getClearAddress().getCountry().getTwoCharacterCode());
    if (StringUtils.isNotEmpty(business.getClearAddress().getStreetLine2())) {
      addressBuilder.setLine2(business.getClearAddress().getStreetLine2());
    }

    companyBuilder
        .setPhone(business.getBusinessPhone().getEncrypted())
        .setAddress(addressBuilder.build());

    accountBuilder.setCompany(companyBuilder.build());

    accountBuilder.setEmail(business.getBusinessEmail().getEncrypted());

    AccountUpdateParams accountUpdateParams =
        accountBuilder
            .setTosAcceptance(
                AccountUpdateParams.TosAcceptance.builder()
                    .setDate(
                        business
                            .getStripeData()
                            .getTosAcceptance()
                            .getDate()
                            .toInstant()
                            .getEpochSecond())
                    .setIp(business.getStripeData().getTosAcceptance().getIp())
                    .setUserAgent(business.getStripeData().getTosAcceptance().getUserAgent())
                    .setServiceAgreement("full")
                    .build())
            .setSettings(
                AccountUpdateParams.Settings.builder()
                    .setCardIssuing(
                        AccountUpdateParams.Settings.CardIssuing.builder()
                            .setTosAcceptance(
                                AccountUpdateParams.Settings.CardIssuing.TosAcceptance.builder()
                                    .setDate(
                                        business
                                            .getStripeData()
                                            .getTosAcceptance()
                                            .getDate()
                                            .toInstant()
                                            .getEpochSecond())
                                    .setIp(business.getStripeData().getTosAcceptance().getIp())
                                    .setUserAgent(
                                        business.getStripeData().getTosAcceptance().getUserAgent())
                                    .build())
                            .build())
                    .putExtraParam(
                        "treasury",
                        Map.of(
                            "tos_acceptance",
                            Map.of(
                                "date",
                                business
                                    .getStripeData()
                                    .getTosAcceptance()
                                    .getDate()
                                    .toInstant()
                                    .getEpochSecond(),
                                "ip",
                                business.getStripeData().getTosAcceptance().getIp(),
                                "user_agent",
                                business.getStripeData().getTosAcceptance().getUserAgent())))
                    .build())
            .build();

    return callStripe(
        "updateAccount",
        accountUpdateParams,
        () ->
            account.update(
                accountUpdateParams, getRequestOptions(new TypedId<>(), 0L, stripeAccountId)));
  }

  public Account retrieveAccount(String stripeAccountId) {
    return callStripe("retrieveAccount", null, () -> Account.retrieve(stripeAccountId));
  }

  public com.clearspend.capital.client.stripe.types.Account retrieveCompleteAccount(
      String stripeAccountId) {
    return callStripeBetaApi(
        "/accounts",
        MultiValueMapBuilder.builder().build(),
        stripeAccountId,
        null,
        com.clearspend.capital.client.stripe.types.Account.class);
  }

  @SneakyThrows
  public Account triggerAccountValidationAfterPersonsProvided(
      String stripeAccountId, Boolean ownersProvided, Boolean executiveProvided) {

    Account account = retrieveAccount(stripeAccountId);

    AccountUpdateParams.Company.Builder companyBuilder = AccountUpdateParams.Company.builder();
    companyBuilder.setOwnersProvided(ownersProvided);
    companyBuilder.setExecutivesProvided(executiveProvided);

    AccountUpdateParams accountUpdateParams =
        new AccountUpdateParams.Builder().setCompany(companyBuilder.build()).build();
    return account.update(
        accountUpdateParams, getRequestOptions(new TypedId<>(), 0L, stripeAccountId));
  }

  public Cardholder createCardholder(
      User user, ClearAddress billingAddress, String stripeAccountId) {
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
            .setPhoneNumber(
                Optional.ofNullable(user.getPhone())
                    .map(NullableEncryptedStringWithHash::getEncrypted)
                    .orElse(null))
            .setStatus(CardholderCreateParams.Status.ACTIVE)
            .setType(CardholderCreateParams.Type.INDIVIDUAL)
            .setBilling(
                CardholderCreateParams.Billing.builder().setAddress(addressBuilder.build()).build())
            .putAllMetadata(
                Map.of(
                    StripeMetadataEntry.BUSINESS_ID.getKey(),
                    user.getBusinessId().toString(),
                    StripeMetadataEntry.STRIPE_ACCOUNT_ID.getKey(),
                    stripeAccountId,
                    StripeMetadataEntry.USER_ID.getKey(),
                    user.getId().toString()))
            .build();

    return callStripe(
        "createCardholder",
        params,
        () ->
            Cardholder.create(
                params,
                getRequestOptions(
                    user.getId(),
                    user.getVersion(),
                    stripeProperties.getClearspendConnectedAccountId())));
  }

  public Person createPerson(BusinessOwner businessOwner, String stripeAccountId) {
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

    // TODO: gb: check cases of inconsistency on stripe
    Relationship.Builder relationship = Relationship.builder().setOwner(true);

    relationship.setOwner(businessOwner.getRelationshipOwner());
    relationship.setRepresentative(businessOwner.getRelationshipRepresentative());
    relationship.setExecutive(businessOwner.getRelationshipExecutive());
    relationship.setDirector(businessOwner.getRelationshipDirector());

    if (businessOwner.getTitle() != null) {
      relationship.setTitle(businessOwner.getTitle());
    }

    if (businessOwner.getPercentageOwnership() != null) {
      relationship.setPercentOwnership(businessOwner.getPercentageOwnership());
    }

    Builder builder =
        PersonCollectionCreateParams.builder()
            .setFirstName(businessOwner.getFirstName().getEncrypted())
            .setLastName(businessOwner.getLastName().getEncrypted())
            .setEmail(businessOwner.getEmail().getEncrypted())
            .setPhone(businessOwner.getPhone().getEncrypted())
            .setRelationship(relationship.build())
            .setAddress(addressBuilder.build())
            .putAllMetadata(
                Map.of(
                    StripeMetadataEntry.BUSINESS_ID.getKey(),
                    businessOwner.getBusinessId().toString(),
                    StripeMetadataEntry.BUSINESS_OWNER_ID.getKey(),
                    businessOwner.getId().toString()));

    if (businessOwner.getDateOfBirth() != null) {
      builder.setDob(
          Dob.builder()
              .setYear((long) businessOwner.getDateOfBirth().getYear())
              .setMonth((long) businessOwner.getDateOfBirth().getMonth().getValue())
              .setDay((long) businessOwner.getDateOfBirth().getDayOfMonth())
              .build());
    }

    // TODO request this in case is mandatory for big company
    // TODO setFullSSN to stripe person
    if (businessOwner.getTaxIdentificationNumber() != null) {
      builder.setIdNumber(businessOwner.getTaxIdentificationNumber().getEncrypted());
      builder.setSsnLast4(
          businessOwner
              .getTaxIdentificationNumber()
              .getEncrypted()
              .substring(businessOwner.getTaxIdentificationNumber().getEncrypted().length() - 4));
    }

    PersonCollectionCreateParams personParameters = builder.build();

    // TODO: Is there a proper api way to get children collection via static methods rather than
    // passing the proper URL?
    PersonCollection personCollection = new PersonCollection();
    personCollection.setUrl("/v1/accounts/%s/persons".formatted(stripeAccountId));
    return callStripe(
        "createPerson",
        personParameters,
        () ->
            personCollection.create(
                personParameters,
                getRequestOptions(new TypedId<>(), businessOwner.getVersion(), stripeAccountId)));
  }

  public Person retrievePerson(String businessOwnerExternalRef, String businessExternalRef) {
    PersonCollection personCollection = new PersonCollection();
    personCollection.setUrl("/v1/accounts/%s/persons".formatted(businessExternalRef));
    return callStripe(
        "retrievePerson", null, () -> personCollection.retrieve(businessOwnerExternalRef));
  }

  @SneakyThrows
  public File uploadFile(MultipartFile file, Purpose purpose, String accountId) {
    log.info("Upload file {} to Stripe", file.getOriginalFilename());
    FileCreateParams fileCreateParams =
        FileCreateParams.builder().setPurpose(purpose).setFile(file.getInputStream()).build();

    return File.create(fileCreateParams, getRequestOptionsBetaApi(new TypedId<>(), accountId));
  }

  @SneakyThrows
  public File uploadFile(InputStream inputStream, Purpose purpose, String accountId) {

    FileCreateParams fileCreateParams =
        FileCreateParams.builder().setPurpose(purpose).setFile(inputStream).build();

    return File.create(fileCreateParams, getRequestOptionsBetaApi(new TypedId<>(), accountId));
  }

  @SneakyThrows
  public Person updatePerson(BusinessOwner businessOwner, String stripeAccountId) {

    Person person = retrievePerson(businessOwner.getStripePersonReference(), stripeAccountId);

    PersonUpdateParams.Address.Builder addressBuilder =
        PersonUpdateParams.Address.builder()
            .setLine1(businessOwner.getAddress().getStreetLine1().getEncrypted())
            .setCity(businessOwner.getAddress().getLocality())
            .setState(businessOwner.getAddress().getRegion())
            .setPostalCode(businessOwner.getAddress().getPostalCode().getEncrypted())
            .setCountry(businessOwner.getAddress().getCountry().getTwoCharacterCode());
    if (StringUtils.isNotEmpty(businessOwner.getAddress().getStreetLine1().getEncrypted())) {
      addressBuilder.setLine2(businessOwner.getAddress().getStreetLine1().getEncrypted());
    }

    // TODO check if id number is mandatory to get from UI
    PersonUpdateParams.Builder builder = PersonUpdateParams.builder();
    builder.setAddress(addressBuilder.build());
    if (!businessOwner.getFirstName().getEncrypted().equals(person.getFirstName())) {
      builder.setFirstName(businessOwner.getFirstName().getEncrypted());
    }
    if (!businessOwner.getLastName().getEncrypted().equals(person.getLastName())) {
      builder.setLastName(businessOwner.getLastName().getEncrypted());
    }
    if (!businessOwner.getEmail().getEncrypted().equals(person.getEmail())) {
      builder.setEmail(businessOwner.getEmail().getEncrypted());
    }
    if (businessOwner.getPhone() != null
        && !businessOwner.getPhone().getEncrypted().equals(person.getPhone())) {
      builder.setPhone(businessOwner.getPhone().getEncrypted());
    }

    PersonUpdateParams.Relationship.Builder builderRelationShip =
        PersonUpdateParams.Relationship.builder();
    builderRelationShip.setOwner(businessOwner.getRelationshipOwner());
    builderRelationShip.setRepresentative(businessOwner.getRelationshipRepresentative());
    builderRelationShip.setExecutive(businessOwner.getRelationshipExecutive());
    builderRelationShip.setDirector(businessOwner.getRelationshipDirector());

    if (businessOwner.getTitle() != null) {
      builderRelationShip.setTitle(businessOwner.getTitle());
    }

    if (businessOwner.getPercentageOwnership() != null) {
      builderRelationShip.setPercentOwnership(businessOwner.getPercentageOwnership());
    }

    builder.setRelationship(builderRelationShip.build());

    if (businessOwner.getDateOfBirth() != null) {
      builder.setDob(
          PersonUpdateParams.Dob.builder()
              .setYear((long) businessOwner.getDateOfBirth().getYear())
              .setMonth((long) businessOwner.getDateOfBirth().getMonth().getValue())
              .setDay((long) businessOwner.getDateOfBirth().getDayOfMonth())
              .build());
    }

    if (businessOwner.getTaxIdentificationNumber() != null) {
      String encrypted = businessOwner.getTaxIdentificationNumber().getEncrypted();
      builder.setSsnLast4(encrypted.substring(encrypted.length() - 4));
      builder.setIdNumber(encrypted);
    }

    return person.update(builder.build());
  }

  public Account updateAccountDocument(
      String accountId, AccountUpdateParams.Company.Verification.Document document) {
    Account account = new Account();
    account.setId(accountId);

    try {
      return account.update(
          AccountUpdateParams.builder()
              .setCompany(
                  AccountUpdateParams.Company.builder()
                      .setVerification(
                          AccountUpdateParams.Company.Verification.builder()
                              .setDocument(document)
                              .build())
                      .build())
              .build());
    } catch (StripeException e) {
      throw new StripeAccountDocumentUpdateException(e.getMessage());
    }
  }

  public Person updatePersonDocuments(String personId, String accountId, Document document) {
    Person person = new Person();
    person.setId(personId);
    person.setAccount(accountId);

    try {
      return person.update(
          PersonUpdateParams.builder()
              .setVerification(Verification.builder().setDocument(document).build())
              .build());
    } catch (StripeException e) {
      throw new StripePersonDocumentUpdateException(e.getMessage());
    }
  }

  @SneakyThrows
  public Person deletePerson(BusinessOwner businessOwner, String stripeAccountId) {

    Person person = new Person();
    person.setId(businessOwner.getStripePersonReference());
    person.setAccount(stripeAccountId);

    return person.delete();
  }

  public Card updateCard(String stripeCardId, CardStatus cardStatus) {
    Card card = new Card();
    card.setId(stripeCardId);

    CardUpdateParams params =
        CardUpdateParams.builder()
            .setStatus(
                switch (cardStatus) {
                  case ACTIVE -> CardUpdateParams.Status.ACTIVE;
                  case INACTIVE -> CardUpdateParams.Status.INACTIVE;
                  case CANCELLED -> CardUpdateParams.Status.CANCELED;
                })
            .build();

    return callStripe(
        "updateCard",
        params,
        () ->
            card.update(
                params,
                getRequestOptions(
                    new TypedId<>(), 0L, stripeProperties.getClearspendConnectedAccountId())));
  }

  public Card createVirtualCard(
      com.clearspend.capital.data.model.Card card, String stripeAccountRef, String stripeUserRef) {
    CardCreateParams cardParameters =
        CardCreateParams.builder()
            .setCardholder(stripeUserRef)
            .setCurrency(Currency.USD.name())
            .setType(CardCreateParams.Type.VIRTUAL)
            .setStatus(Status.ACTIVE)
            .setSpendingControls(
                SpendingControls.builder()
                    .addSpendingLimit(
                        SpendingLimit.builder()
                            .setAmount(10_000_00L)
                            .setInterval(Interval.DAILY)
                            .build())
                    .build())
            .putExtraParam("financial_account", stripeProperties.getClearspendFinancialAccountId())
            .putMetadata(StripeMetadataEntry.BUSINESS_ID.getKey(), card.getBusinessId().toString())
            .putMetadata(StripeMetadataEntry.CARD_ID.getKey(), card.getId().toString())
            .putMetadata(StripeMetadataEntry.STRIPE_ACCOUNT_ID.getKey(), stripeAccountRef)
            .build();
    log.debug("Virtual card: cardParameters: {}", cardParameters);

    return callStripe(
        "createCard",
        cardParameters,
        () ->
            com.stripe.model.issuing.Card.create(
                cardParameters,
                getRequestOptionsBetaApi(
                    card.getId(), stripeProperties.getClearspendConnectedAccountId())));
  }

  public Card createPhysicalCard(
      com.clearspend.capital.data.model.Card card,
      com.clearspend.capital.common.data.model.Address shippingAddress,
      String shippingLabel,
      String stripeAccountRef,
      String stripeUserRef) {
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
            .setCardholder(stripeUserRef)
            .setCurrency(Currency.USD.name())
            .setType(CardCreateParams.Type.PHYSICAL)
            .setStatus(Status.INACTIVE)
            .setShipping(
                Shipping.builder()
                    .setName(shippingLabel)
                    .setService(Service.STANDARD)
                    .setAddress(addressBuilder.build())
                    .build())
            .putExtraParam("financial_account", stripeProperties.getClearspendFinancialAccountId())
            .setSpendingControls(
                SpendingControls.builder()
                    .addSpendingLimit(
                        SpendingLimit.builder()
                            .setAmount(10_000_00L)
                            .setInterval(Interval.DAILY)
                            .build())
                    .build())
            .putMetadata(StripeMetadataEntry.BUSINESS_ID.getKey(), card.getBusinessId().toString())
            .putMetadata(StripeMetadataEntry.CARD_ID.getKey(), card.getId().toString())
            .putMetadata(StripeMetadataEntry.STRIPE_ACCOUNT_ID.getKey(), stripeAccountRef)
            .build();
    log.debug("Physical card: cardParameters: {}", cardParameters);

    return callStripe(
        "createCard",
        cardParameters,
        () ->
            com.stripe.model.issuing.Card.create(
                cardParameters,
                getRequestOptionsBetaApi(
                    card.getId(), stripeProperties.getClearspendConnectedAccountId())));
  }

  public FinancialAccount createFinancialAccount(
      TypedId<BusinessId> businessId, String accountExternalRef) {
    MultiValueMapBuilder multiValueMapBuilder =
        MultiValueMapBuilder.builder()
            .add("supported_currencies[]", "usd")
            .add("features[inbound_transfers][ach][requested]", "true")
            .add("features[card_issuing][requested]", "true")
            .add("features[deposit_insurance][requested]", "true")
            .add("features[financial_addresses][aba][requested]", "true")
            .add("features[intra_stripe_flows][requested]", "true")
            .add("features[outbound_payments][ach][requested]", "true")
            .add("features[outbound_payments][us_domestic_wire][requested]", "true")
            .add("features[outbound_transfers][ach][requested]", "true")
            .add("features[outbound_transfers][us_domestic_wire][requested]", "true")
            .add("features[inbound_transfers][ach][requested]", "true")
            .addMetadata(StripeMetadataEntry.BUSINESS_ID, businessId);

    return callStripeBetaApi(
        "/financial_accounts",
        multiValueMapBuilder.build(),
        accountExternalRef,
        "fa" + businessId,
        FinancialAccount.class);
  }

  public FinancialAccount getFinancialAccount(
      TypedId<BusinessId> businessId, String stripeAccountRef, String stripeFinancialAccountRef) {
    MultiValueMapBuilder multiValueMapBuilder =
        MultiValueMapBuilder.builder()
            .add("expand[]", "financial_addresses.aba.account_number")
            .addMetadata(StripeMetadataEntry.BUSINESS_ID, businessId);

    return callStripeBetaApi(
        "/financial_accounts/%s".formatted(stripeFinancialAccountRef),
        multiValueMapBuilder.build(),
        stripeAccountRef,
        new TypedId<>().toString(),
        FinancialAccount.class);
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
      if (log.isInfoEnabled()) {
        String resultStr = null;
        try {
          resultStr = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
          // do nothing
        }
        log.info(
            "Calling stripe [%s] method. \n Request: %s, \n Response: %s"
                .formatted(methodName, request, resultStr != null ? resultStr : result));
      }
    }

    return result;
  }

  private <T> T callStripeBetaApi(
      String uri,
      MultiValueMap<String, String> parameters,
      String stripeAccountId,
      String idempotencyKey,
      Class<T> clazz) {
    T result;
    String loggedResult = null;

    Objects.requireNonNull(
        parameters.get(
            MultiValueMapBuilder.METADATA_KEY_FORMAT.formatted(
                StripeMetadataEntry.BUSINESS_ID.getKey())));

    try {
      result =
          stripeTreasuryWebClient
              .post()
              .uri(uri)
              .headers(
                  httpHeaders -> {
                    httpHeaders.add("Stripe-Account", stripeAccountId);
                    httpHeaders.add("Stripe-Version", STRIPE_BETA_HEADER);
                    httpHeaders.add("Idempotency-Key", idempotencyKey);
                  })
              .body(BodyInserters.fromFormData(parameters))
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(clazz);
                    }

                    return response.createException().flatMap(Mono::error);
                  })
              .block();
      loggedResult = result != null ? result.toString() : null;

      return result;
    } catch (WebClientResponseException e) {
      loggedResult = e.getResponseBodyAsString();
      throw e;
    } finally {
      if (log.isInfoEnabled()) {
        String requestStr = null;
        try {
          requestStr = objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
          // do nothing
        }
        log.info(
            "Calling stripe [%s] beta method. \n Request: %s, \n Response: %s"
                .formatted(
                    uri, requestStr != null ? requestStr : parameters.toString(), loggedResult));
      }
    }
  }

  // see https://stripe.com/docs/api/idempotent_requests
  private RequestOptions getRequestOptions(
      TypedId<?> idempotencyKey, Long version, String stripeAccountId) {
    RequestOptionsBuilder builder =
        RequestOptions.builder().setIdempotencyKey(idempotencyKey.toString() + version);

    BeanUtils.setNotNull(stripeAccountId, builder::setStripeAccount);

    return builder.build();
  }

  private RequestOptions getRequestOptionsBetaApi(
      TypedId<?> idempotencyKey, String stripeAccountId) {
    RequestOptionsBuilder builder =
        RequestOptions.builder()
            .setStripeVersionOverride(STRIPE_BETA_HEADER)
            .setIdempotencyKey(idempotencyKey.toString());

    BeanUtils.setNotNull(stripeAccountId, builder::setStripeAccount);

    return builder.build();
  }

  /**
   * Returns an ephemeral key which frontend can use to reveal the Virtual card details in the
   * PCI-compliant way
   */
  public String getEphemeralKey(String cardId, String nonce) {
    try {
      return EphemeralKey.create(
              Map.of("issuing_card", cardId, "nonce", nonce),
              RequestOptions.builder()
                  .setIdempotencyKey(cardId + nonce)
                  .setStripeVersionOverride("2020-03-02")
                  .setStripeAccount(stripeProperties.getClearspendConnectedAccountId())
                  .build())
          .getSecret();
    } catch (StripeException e) {
      throw new StripeClientException(e);
    }
  }

  /** Returns an ephemeral key for a provided card id */
  public EphemeralKey getEphemeralKeyObjectForCard(String cardId, String apiVersion) {
    try {
      return EphemeralKey.create(
          Map.of("issuing_card", cardId),
          RequestOptions.builder()
              .setStripeVersionOverride(apiVersion)
              .setStripeAccount(stripeProperties.getClearspendConnectedAccountId())
              .build());
    } catch (StripeException e) {
      throw new StripeClientException(e);
    }
  }

  public Account setExternalAccount(String accountId, String btok) {
    Account account = new Account();
    account.setId(accountId);
    AccountUpdateParams params = AccountUpdateParams.builder().setExternalAccount(btok).build();

    return callStripe(
        "updateAccount",
        params,
        () ->
            account.update(
                params, RequestOptions.builder().setIdempotencyKey(accountId + btok).build()));
  }

  public SetupIntent createSetupIntent(
      String stripeAccountId,
      String bankAccountId,
      String customerAcceptanceIpAddress,
      String customerAcceptanceUserAgent) {

    CustomerAcceptance customerAcceptance =
        CustomerAcceptance.builder()
            .setType(CustomerAcceptance.Type.ONLINE)
            .setOnline(
                CustomerAcceptance.Online.builder()
                    .setIpAddress(customerAcceptanceIpAddress)
                    .setUserAgent(customerAcceptanceUserAgent)
                    .build())
            .build();

    SetupIntentCreateParams params =
        SetupIntentCreateParams.builder()
            .setMandateData(MandateData.builder().setCustomerAcceptance(customerAcceptance).build())
            .setPaymentMethod(bankAccountId)
            .setConfirm(true)
            .addPaymentMethodType("us_bank_account")
            .putExtraParam("attach_to_self", true)
            .build();

    SetupIntent setupIntent =
        callStripe(
            "createSetupIntent",
            params,
            () ->
                SetupIntent.create(
                    params,
                    RequestOptions.builder()
                        .setStripeAccount(stripeAccountId)
                        .setIdempotencyKey(bankAccountId)
                        .setStripeVersionOverride(STRIPE_BETA_HEADER)
                        .build()));

    if (!setupIntent.getStatus().equals("succeeded")) {
      throw new RuntimeException(
          "Error creating setup intent for stripe bank account id " + bankAccountId);
    }

    return setupIntent;
  }

  public InboundTransfer executeInboundTransfer(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      String stripeAccountRef,
      String stripeBankAccountRef,
      String stripeFinancialAccountRef,
      Amount amount,
      String description,
      String statementDescriptor) {

    String paymentMethod = stripeBankAccountRef;
    if (stripeProperties.isEnableTransferFailures()) {
      paymentMethod =
          switch ((int) amount.toStripeAmount()) {
            case 13100 -> "pm_usBankAccount_noAccount";
            case 13200 -> "pm_usBankAccount_invalidAccountNumber";
            case 13300 -> "pm_usBankAccount_dispute";
            default -> paymentMethod;
          };
    }

    MultiValueMap<String, String> formData =
        MultiValueMapBuilder.builder()
            .add("origin_payment_method", paymentMethod)
            .add("financial_account", stripeFinancialAccountRef)
            .add("amount", Long.toString(amount.toStripeAmount()))
            .add("description", description)
            .add("statement_descriptor", statementDescriptor)
            .add("currency", amount.getCurrency().toStripeCurrency())
            .addMetadata(StripeMetadataEntry.BUSINESS_ID, businessId)
            .addMetadata(StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID, businessBankAccountId)
            .addMetadata(StripeMetadataEntry.ADJUSTMENT_ID, adjustmentId)
            .addMetadata(StripeMetadataEntry.HOLD_ID, holdId)
            .build();

    return callStripeBetaApi(
        "/inbound_transfers",
        formData,
        stripeAccountRef,
        "it_" + adjustmentId,
        InboundTransfer.class);
  }

  public OutboundTransfer executeOutboundTransfer(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      String stripeAccountRef,
      String stripeFinancialAccountRef,
      String stripeBankAccountRef,
      Amount amount,
      String description,
      String statementDescriptor) {

    String paymentMethod = stripeBankAccountRef;
    if (stripeProperties.isEnableTransferFailures()) {
      paymentMethod =
          switch ((int) amount.toStripeAmount()) {
            case 14100 -> "pm_usBankAccount_canceledByUser";
            case 14200 -> "pm_usBankAccount_internalFailure";
            case 14300 -> "pm_usBankAccount_accountClosed";
            case 14400 -> "pm_usBankAccount_invalidAccountNumber";
            default -> paymentMethod;
          };
    }

    MultiValueMap<String, String> formData =
        MultiValueMapBuilder.builder()
            .add("destination_payment_method", paymentMethod)
            .add("financial_account", stripeFinancialAccountRef)
            .add("amount", Long.toString(amount.toStripeAmount()))
            .add("description", description)
            .add("statement_descriptor", statementDescriptor)
            .add("currency", amount.getCurrency().toStripeCurrency())
            .addMetadata(StripeMetadataEntry.BUSINESS_ID, businessId)
            .addMetadata(StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID, businessBankAccountId)
            .build();

    return callStripeBetaApi(
        "/outbound_transfers", formData, stripeAccountRef, null, OutboundTransfer.class);
  }

  public OutboundPayment pushFundsToConnectedFinancialAccount(
      TypedId<BusinessId> businessId,
      String toStripeFinancialAccountRef,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount,
      String description,
      String statementDescriptor) {

    return executeOutboundPayment(
        businessId,
        stripeProperties.getClearspendConnectedAccountId(),
        stripeProperties.getClearspendFinancialAccountId(),
        toStripeFinancialAccountRef,
        adjustmentId,
        amount,
        description,
        statementDescriptor);
  }

  public OutboundPayment pushFundsToClearspendFinancialAccount(
      TypedId<BusinessId> businessId,
      String fromAccountRef,
      String fromFinancialAccountRef,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount,
      String description,
      String statementDescriptor) {

    return executeOutboundPayment(
        businessId,
        fromAccountRef,
        fromFinancialAccountRef,
        stripeProperties.getClearspendFinancialAccountId(),
        adjustmentId,
        amount,
        description,
        statementDescriptor);
  }

  private OutboundPayment executeOutboundPayment(
      TypedId<BusinessId> businessId,
      String fromAccountRef,
      String fromFinancialAccountRef,
      String toFinancialAccountRef,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount,
      String description,
      String statementDescriptor) {
    MultiValueMap<String, String> formData =
        MultiValueMapBuilder.builder()
            .add("financial_account", fromFinancialAccountRef)
            .add("amount", Long.toString(amount.toStripeAmount()))
            .add("currency", amount.getCurrency().toStripeCurrency())
            .add("destination_payment_method_data[type]", "financial_account")
            .add("destination_payment_method_data[financial_account]", toFinancialAccountRef)
            .add("description", description)
            .add("statement_descriptor", statementDescriptor)
            .add("end_user_details[present]", "false")
            .addMetadata(StripeMetadataEntry.BUSINESS_ID, businessId)
            .addMetadata(StripeMetadataEntry.ADJUSTMENT_ID, adjustmentId)
            .build();

    return callStripeBetaApi(
        "/outbound_payments",
        formData,
        fromAccountRef,
        "sp_" + adjustmentId,
        OutboundPayment.class);
  }

  public void declineAuthorization(Authorization authorization, NetworkCommon networkCommon) {
    AuthorizationDeclineParams params =
        AuthorizationDeclineParams.builder().setMetadata(networkCommon.getMetadata()).build();

    callStripe(
        "authorizationDecline",
        params,
        () ->
            authorization.decline(
                params,
                RequestOptions.builder()
                    .setStripeAccount(stripeProperties.getClearspendConnectedAccountId())
                    .build()));
  }

  public void approveAuthorization(Authorization authorization, NetworkCommon networkCommon) {
    authorization.setApproved(true);
    AuthorizationApproveParams.Builder builder =
        AuthorizationApproveParams.builder().setMetadata(networkCommon.getMetadata());
    if (networkCommon.isAllowPartialApproval()) {
      // amounts going back to Stripe for authorizations should be positive
      builder.setAmount(networkCommon.getApprovedAmount().abs().toStripeAmount());
    }

    AuthorizationApproveParams params = builder.build();

    callStripe(
        "authorizationApprove",
        params,
        () ->
            authorization.approve(
                params,
                RequestOptions.builder()
                    .setStripeAccount(stripeProperties.getClearspendConnectedAccountId())
                    .build()));
  }
}
