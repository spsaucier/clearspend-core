package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.MccGroup;
import com.clearspend.capital.data.model.enums.I2CMccGroup;
import com.clearspend.capital.data.repository.MccGroupRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MccGroupService {

  /** Keeping all groups in cache since they are read only */
  private final ConcurrentHashMap<TypedId<BusinessId>, List<MccGroup>> mccGroupsCache =
      new ConcurrentHashMap<>();

  private final MccGroupRepository mccGroupRepository;

  @Transactional
  public void initializeMccGroups(TypedId<BusinessId> businessId) {
    if (mccGroupRepository.countByBusinessId(businessId) == 0) {
      Arrays.stream(I2CMccGroup.values())
          .forEach(
              value ->
                  mccGroupRepository.save(
                      new MccGroup(
                          value.getI2cName(), Collections.emptyList(), value, businessId)));
    }
  }

  public List<MccGroup> retrieveMccGroups(TypedId<BusinessId> businessId) {
    return mccGroupsCache.computeIfAbsent(businessId, mccGroupRepository::findByBusinessId);
  }
}
