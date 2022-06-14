package com.clearspend.capital.controller.type.notification;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.Amount;
import java.util.Set;
import lombok.NonNull;

public record AllocationNotificationsSettingRequest(
    @NonNull TypedId<AllocationId> allocationId,
    boolean lowBalance,
    @NonNull Amount lowBalanceLevel,
    @NonNull Set<TypedId<UserId>> recipients) {}
