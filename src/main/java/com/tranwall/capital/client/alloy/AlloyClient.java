package com.tranwall.capital.client.alloy;

import com.tranwall.capital.client.alloy.request.OnboardBusinessRequest;
import com.tranwall.capital.client.alloy.request.OnboardIndividualRequest;
import com.tranwall.capital.client.alloy.response.OnboardResponse;
import com.tranwall.capital.common.data.State;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AlloyClient {
  private static final String US_COUNTRY_CODE = "US";

  private final WebClient individualWebClient;
  private final WebClient businessWebClient;

  public AlloyClient(
      @Qualifier("alloyIndividualWebClient") WebClient individualWebClient,
      @Qualifier("alloyBusinessWebClient") WebClient businessWebClient) {
    this.individualWebClient = individualWebClient;
    this.businessWebClient = businessWebClient;
  }

  public KnowYourCustomerStatus onboardIndividual(BusinessOwner owner) {
    State state = State.valueOfRegion(owner.getAddress().getRegion());
    if (state == State.UNKNOWN) {
      throw new ValidationException("Unknown region provided: " + owner.getAddress().getRegion());
    }

    OnboardIndividualRequest request =
        new OnboardIndividualRequest(
            owner.getFirstName().getEncrypted(),
            owner.getLastName().getEncrypted(),
            owner.getEmail().getEncrypted(),
            owner.getAddress().getStreetLine1().getEncrypted(),
            owner.getAddress().getLocality(),
            state.getAbbreviation(),
            owner.getAddress().getPostalCode().getEncrypted(),
            US_COUNTRY_CODE,
            owner.getPhone().getEncrypted(),
            owner.getDateOfBirth());

    String outcome = callEvaluationsEndpoint(individualWebClient, request);

    return toKnowYourCustomerStatus(outcome);
  }

  public KnowYourBusinessStatus onboardBusiness(Business business) {
    State state = State.valueOfRegion(business.getClearAddress().getRegion());
    if (state == State.UNKNOWN) {
      throw new ValidationException(
          "Unknown region provided: " + business.getClearAddress().getRegion());
    }

    OnboardBusinessRequest request =
        new OnboardBusinessRequest(
            business.getLegalName(),
            business.getClearAddress().getStreetLine1(),
            business.getClearAddress().getLocality(),
            state.getAbbreviation(),
            business.getClearAddress().getPostalCode(),
            business.getEmployerIdentificationNumber(),
            business.getBusinessPhone().getEncrypted());

    request.setBusinessAddressLine2(business.getClearAddress().getStreetLine2());

    String outcome = callEvaluationsEndpoint(businessWebClient, request);

    return toKnowYourBusinessStatus(outcome);
  }

  private String callEvaluationsEndpoint(WebClient client, Object request) {
    OnboardResponse response =
        client
            .post()
            .uri("evaluations")
            .body(BodyInserters.fromValue(request))
            .retrieve()
            .bodyToMono(OnboardResponse.class)
            .block();

    if (response == null
        || response.getSummary() == null
        || StringUtils.isNotBlank(response.getError())) {
      throw new RuntimeException(
          String.format("Failed to call Alloy evaluation endpoint. Response: [%s]", response));
    }

    return response.getSummary().getOutcome();
  }

  private KnowYourBusinessStatus toKnowYourBusinessStatus(String outcome) {
    return switch (outcome) {
      case "Approved" -> KnowYourBusinessStatus.PASS;
      case "Manual Review" -> KnowYourBusinessStatus.REVIEW;
      case "Denied" -> KnowYourBusinessStatus.FAIL;
      default -> throw new RuntimeException(
          String.format("Business evaluation state: [%s] is not supported", outcome));
    };
  }

  private KnowYourCustomerStatus toKnowYourCustomerStatus(String outcome) {
    return switch (outcome) {
      case "Approved" -> KnowYourCustomerStatus.PASS;
      case "Manual Review" -> KnowYourCustomerStatus.REVIEW;
      case "Denied" -> KnowYourCustomerStatus.FAIL;
      default -> throw new RuntimeException(
          String.format("Customer evaluation state: [%s] is not supported", outcome));
    };
  }
}
