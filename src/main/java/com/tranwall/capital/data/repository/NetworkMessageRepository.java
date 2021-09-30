package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.NetworkMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkMessageRepository extends JpaRepository<NetworkMessage, UUID> {}
