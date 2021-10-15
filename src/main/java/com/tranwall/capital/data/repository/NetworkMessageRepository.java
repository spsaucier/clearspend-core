package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.NetworkMessageId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.NetworkMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkMessageRepository
    extends JpaRepository<NetworkMessage, TypedId<NetworkMessageId>> {}
