package com.clearspend.capital.data.repository.network;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.network.NetworkMessageId;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.NetworkMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkMessageRepository
    extends JpaRepository<NetworkMessage, TypedId<NetworkMessageId>> {

  // method used in testing only
  int countByNetworkMessageGroupId(UUID networkMessageGroupId);

  List<NetworkMessage> findByExternalRefAndTypeOrderByCreatedDesc(
      String externalRef, NetworkMessageType type);
}
