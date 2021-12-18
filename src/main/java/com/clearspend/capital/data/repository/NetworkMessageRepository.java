package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.NetworkMessageId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.NetworkMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkMessageRepository
    extends JpaRepository<NetworkMessage, TypedId<NetworkMessageId>> {}
