package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.NetworkMessageId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.NetworkMessage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkMessageRepository
    extends JpaRepository<NetworkMessage, TypedId<NetworkMessageId>> {

  // method used in testing only
  int countByNetworkMessageGroupId(UUID networkMessageGroupId);

  Optional<NetworkMessage> findByExternalRef(String externalRef);
}
