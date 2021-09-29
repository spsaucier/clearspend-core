package com.tranwall.data.repository;

import com.tranwall.data.model.Hold;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<Hold, UUID> {}
