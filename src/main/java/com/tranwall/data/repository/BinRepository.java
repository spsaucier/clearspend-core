package com.tranwall.data.repository;

import com.tranwall.data.model.Bin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinRepository extends JpaRepository<Bin, UUID> {}
