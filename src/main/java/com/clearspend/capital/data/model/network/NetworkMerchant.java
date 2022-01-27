package com.clearspend.capital.data.model.network;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.network.NetworkMerchantId;
import javax.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
// This table holds all unique merchants (unique tuple name, MCC and type). We will add the limits
// fields later.
public class NetworkMerchant extends TypedMutable<NetworkMerchantId> {

  // the name of the merchant from the location field in the ISO message
  @NonNull private String merchantName;

  // a.k.a. MCC (4 digits)
  @NonNull private Integer merchantCategoryCode;
}
