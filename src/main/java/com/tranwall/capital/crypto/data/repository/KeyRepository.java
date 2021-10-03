package com.tranwall.capital.crypto.data.repository;

import com.tranwall.capital.crypto.data.model.Key;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyRepository extends JpaRepository<Key, UUID> {}
