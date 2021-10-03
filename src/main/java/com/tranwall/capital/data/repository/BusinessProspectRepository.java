package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.BusinessProspect;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProspectRepository extends JpaRepository<BusinessProspect, UUID> {}
