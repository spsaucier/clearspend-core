package com.clearspend.capital.client.alloy;

import com.clearspend.capital.client.alloy.request.DescribeDocumentRequest;
import com.clearspend.capital.client.alloy.request.DocumentExtension;
import com.clearspend.capital.client.alloy.request.DocumentType;
import com.clearspend.capital.client.alloy.request.OnboardBusinessRequest;
import com.clearspend.capital.client.alloy.request.OnboardIndividualRequest;
import com.clearspend.capital.client.alloy.request.State;
import com.clearspend.capital.client.alloy.response.DocumentResponse;
import com.clearspend.capital.client.alloy.response.EntityInformation;
import com.clearspend.capital.client.alloy.response.OnboardResponse;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Profile("!test")
@Component
public class AlloyClient {
  private static final String US_COUNTRY_CODE = "US";

  private final WebClient individualWebClient;
  private final WebClient businessWebClient;
  private final WebClient documentWebClient;
  private final WebClient groupEvaluationWebClient;

  public AlloyClient(
      @Qualifier("alloyIndividualWebClient") WebClient individualWebClient,
      @Qualifier("alloyBusinessWebClient") WebClient businessWebClient,
      @Qualifier("alloyDocumentWebClient") WebClient documentWebClient,
      @Qualifier("alloyGroupWebClient") WebClient groupEvaluationWebClient) {
    this.individualWebClient = individualWebClient;
    this.businessWebClient = businessWebClient;
    this.documentWebClient = documentWebClient;
    this.groupEvaluationWebClient = groupEvaluationWebClient;
  }

  public record KycEvaluationResponse(
      String entityToken, KnowYourCustomerStatus status, List<String> reasons) {}

  public record KybEvaluationResponse(
      String entityToken, KnowYourBusinessStatus status, List<String> reasons) {}

  public KycEvaluationResponse onboardIndividual(BusinessOwner owner, String alloyGroup) {
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

    OnboardResponse response = callEvaluationsEndpoint(individualWebClient, request, alloyGroup);

    OnboardResponse groupEvaluationResponse = runGroupEvaluation(alloyGroup);

    if (!response
        .getSummary()
        .getOutcome()
        .equals(groupEvaluationResponse.getSummary().getOutcome())) {
      response = groupEvaluationResponse;
    }

    return new KycEvaluationResponse(
        response.getEntityToken(),
        toKnowYourCustomerStatus(response.getSummary().getOutcome()),
        ObjectUtils.firstNonNull(
            response.getSummary().getOutcomeReasons(), Collections.emptyList()));
  }

  public KybEvaluationResponse onboardBusiness(Business business) {
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

    String group = business.getLegalName().replaceAll(" ", "") + business.getBusinessPhone();
    OnboardResponse response = callEvaluationsEndpoint(businessWebClient, request, group);

    return new KybEvaluationResponse(
        response.getEntityToken(),
        toKnowYourBusinessStatus(response.getSummary().getOutcome()),
        ObjectUtils.firstNonNull(
            response.getSummary().getOutcomeReasons(), Collections.emptyList()));
  }

  public OnboardResponse runGroupEvaluation(String groupExternalToken) {
    return groupEvaluationWebClient
        .post()
        .uri("groups/" + groupExternalToken + "/evaluations")
        .retrieve()
        .bodyToMono(OnboardResponse.class)
        .block();
  }

  public EntityInformation getEntityInformationForBusinessEntity(String entityToken) {
    return businessWebClient
        .get()
        .uri("entities/" + entityToken)
        .retrieve()
        .bodyToMono(EntityInformation.class)
        .block();
  }

  public EntityInformation getEntityInformationForIndividualEntity(String entityToken) {
    return individualWebClient
        .get()
        .uri("entities/" + entityToken)
        .retrieve()
        .bodyToMono(EntityInformation.class)
        .block();
  }

  public OnboardResponse getEvaluationForBusinessEntity(
      String entityToken, String evaluationToken) {
    return businessWebClient
        .get()
        .uri("entities/" + entityToken + "/evaluations/" + evaluationToken)
        .retrieve()
        .bodyToMono(OnboardResponse.class)
        .block();
  }

  public OnboardResponse getEvaluationForIndividualEntity(
      String entityToken, String evaluationToken) {
    return individualWebClient
        .get()
        .uri("entities/" + entityToken + "/evaluations/" + evaluationToken)
        .retrieve()
        .bodyToMono(OnboardResponse.class)
        .block();
  }

  public void uploadDocument(MultipartFile multipartFile, String group) {
    String[] strings = Objects.requireNonNull(multipartFile.getOriginalFilename()).split("\\|");
    DocumentType documentType = DocumentType.valueOf(strings[1]);
    String entityToken = strings[0];
    String fileName = strings[2];
    DescribeDocumentRequest request =
        new DescribeDocumentRequest(
            fileName,
            DocumentExtension.getExtensionByContentType(multipartFile.getContentType()),
            documentType);

    DocumentResponse documentResponseDetails =
        documentWebClient
            .post()
            .uri("entities/" + entityToken + "/documents")
            .header("Alloy-External-Group-ID", group)
            .body(BodyInserters.fromValue(request))
            .retrieve()
            .bodyToMono(DocumentResponse.class)
            .block();

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", multipartFile.getResource());

    assert documentResponseDetails != null;

    documentWebClient
        .put()
        .uri("entities/" + entityToken + "/documents/" + documentResponseDetails.getDocumentToken())
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .header("Alloy-External-Group-ID", group)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .retrieve()
        .bodyToMono(DocumentResponse.class)
        .block();
  }

  private OnboardResponse callEvaluationsEndpoint(WebClient client, Object request, String group) {
    OnboardResponse response =
        client
            .post()
            .uri("evaluations")
            .header("Alloy-External-Group-ID", group)
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

    return response;
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
