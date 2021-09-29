package com.tranwall.data.repository;

import com.tranwall.data.model.Card;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {}
