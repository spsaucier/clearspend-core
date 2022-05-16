package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import java.util.Set;

public record ArchiveAllocationResponse(Set<TypedId<AllocationId>> archivedAllocationIds) {}
