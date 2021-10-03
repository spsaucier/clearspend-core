package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Business;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, UUID> {}
