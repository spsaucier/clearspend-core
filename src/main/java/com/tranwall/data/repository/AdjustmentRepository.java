package com.tranwall.data.repository;

import com.tranwall.data.model.Adjustment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjustmentRepository extends JpaRepository<Adjustment, UUID> {}
