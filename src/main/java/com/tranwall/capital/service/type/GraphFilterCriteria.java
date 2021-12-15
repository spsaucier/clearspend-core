package com.tranwall.capital.service.type;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class GraphFilterCriteria {

  private TypedId<AllocationId> allocationId;
  private TypedId<UserId> userId;
  @NonNull @NotNull private OffsetDateTime from;
  @NonNull @NotNull private OffsetDateTime to;
}
