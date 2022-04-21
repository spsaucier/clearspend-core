package com.clearspend.capital.controller.type.plaid;

import com.clearspend.capital.common.typedid.data.PlaidLogEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.PlaidAccessTokenLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.PlaidAccountLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.PlaidBalanceLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.PlaidLinkTokenLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.PlaidOwnerLogEntryDetails;
import com.clearspend.capital.controller.type.plaid.PlaidLogEntryDetails.SandboxLinkTokenLogEntryDetails;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.AuthGetResponse;
import com.plaid.client.model.IdentityGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    subTypes = {
      PlaidBalanceLogEntryDetails.class,
      PlaidOwnerLogEntryDetails.class,
      PlaidAccountLogEntryDetails.class,
      PlaidLinkTokenLogEntryDetails.class,
      PlaidAccessTokenLogEntryDetails.class,
      SandboxLinkTokenLogEntryDetails.class
    },
    discriminatorProperty = "plaidResponseType",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "BALANCE", schema = PlaidBalanceLogEntryDetails.class),
      @DiscriminatorMapping(value = "OWNER", schema = PlaidOwnerLogEntryDetails.class),
      @DiscriminatorMapping(value = "ACCOUNT", schema = PlaidAccountLogEntryDetails.class),
      @DiscriminatorMapping(value = "LINK_TOKEN", schema = PlaidLinkTokenLogEntryDetails.class),
      @DiscriminatorMapping(value = "ACCESS_TOKEN", schema = PlaidAccessTokenLogEntryDetails.class),
      @DiscriminatorMapping(
          value = "SANDBOX_LINK_TOKEN",
          schema = SandboxLinkTokenLogEntryDetails.class)
    })
public abstract class PlaidLogEntryDetails<T> {
  @NonNull private TypedId<PlaidLogEntryId> id;
  @NonNull private TypedId<BusinessId> businessId;
  @NonNull private OffsetDateTime created;
  @NonNull private PlaidResponseType plaidResponseType;
  @NonNull private T message;

  @SneakyThrows
  public static <T> PlaidLogEntryDetails<T> fromPlaidLogEntry(
      final ObjectMapper objectMapper, final PlaidLogEntry plaidLogEntry) {
    final T message =
        (T)
            objectMapper.readValue(
                plaidLogEntry.getMessage().getEncrypted(),
                plaidLogEntry.getPlaidResponseType().getResponseClass());
    return new DefaultPlaidLogEntryDetails<>(
        plaidLogEntry.getId(),
        plaidLogEntry.getBusinessId(),
        plaidLogEntry.getCreated(),
        plaidLogEntry.getPlaidResponseType(),
        message);
  }

  /**
   * Base class has to be abstract for Swagger purposes, this one exists simply to help instantiate
   * it.
   */
  public static class DefaultPlaidLogEntryDetails<T> extends PlaidLogEntryDetails<T> {
    public DefaultPlaidLogEntryDetails(
        @NonNull TypedId<PlaidLogEntryId> id,
        @NonNull TypedId<BusinessId> businessId,
        @NonNull OffsetDateTime created,
        @NonNull PlaidResponseType plaidResponseType,
        @NonNull T message) {
      super(id, businessId, created, plaidResponseType, message);
    }

    public DefaultPlaidLogEntryDetails() {}
  }

  // The following types primarily exist to support Swagger definitions
  public static class PlaidBalanceLogEntryDetails
      extends PlaidLogEntryDetails<AccountsGetResponse> {}

  public static class PlaidOwnerLogEntryDetails extends PlaidLogEntryDetails<IdentityGetResponse> {}

  public static class PlaidAccountLogEntryDetails extends PlaidLogEntryDetails<AuthGetResponse> {}

  public static class PlaidLinkTokenLogEntryDetails
      extends PlaidLogEntryDetails<LinkTokenCreateResponse> {}

  public static class PlaidAccessTokenLogEntryDetails
      extends PlaidLogEntryDetails<ItemPublicTokenExchangeResponse> {}

  public static class SandboxLinkTokenLogEntryDetails
      extends PlaidLogEntryDetails<SandboxPublicTokenCreateResponse> {}
}
