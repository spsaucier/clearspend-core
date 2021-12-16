package com.tranwall.capital.data.model.enums;

import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.AuthGetResponse;
import com.plaid.client.model.IdentityGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;

public enum PlaidResponseType {
  BALANCE(AccountsGetResponse.class),
  OWNER(IdentityGetResponse.class),
  ACCOUNT(AuthGetResponse.class),
  LINK_TOKEN(LinkTokenCreateResponse.class),
  ACCESS_TOKEN(ItemPublicTokenExchangeResponse.class),
  SANDBOX_LINK_TOKEN(SandboxPublicTokenCreateResponse.class),
  ERROR(Error.class),
  OTHER(Object.class);

  public final Class<?> responseClass;

  PlaidResponseType(Class<?> responseClass) {
    this.responseClass = responseClass;
  }
}
