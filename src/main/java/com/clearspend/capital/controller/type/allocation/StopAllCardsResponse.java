package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import java.util.Set;
import lombok.NonNull;

public record StopAllCardsResponse(
    @NonNull Set<TypedId<CardId>> cancelledCards, @NonNull Set<TypedId<CardId>> unlinkedCards) {}
