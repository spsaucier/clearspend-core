package com.tranwall.data.repository;

import com.tranwall.data.model.BusinessOwner;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessOwnerRepository extends JpaRepository<BusinessOwner, UUID> {}
