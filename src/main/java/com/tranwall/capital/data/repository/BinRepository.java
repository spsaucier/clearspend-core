package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Bin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinRepository extends JpaRepository<Bin, UUID> {}
