package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Adjustment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjustmentRepository extends JpaRepository<Adjustment, UUID> {}
