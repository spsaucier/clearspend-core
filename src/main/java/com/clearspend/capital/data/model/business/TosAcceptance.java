package com.clearspend.capital.data.model.business;

import java.time.OffsetDateTime;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@RequiredArgsConstructor
@MappedSuperclass
public class TosAcceptance {

  // date of tos acceptance
  @NonNull private OffsetDateTime date;
  // on business creation we will collect the ip of the customer, required by Stripe
  @NonNull private String ip;
  // on business creation we will collect the userAgent of the customer, as part of tos acceptance
  @NonNull private String userAgent;
}
