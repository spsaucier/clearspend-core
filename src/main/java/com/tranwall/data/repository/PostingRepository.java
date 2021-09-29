package com.tranwall.data.repository;

import com.tranwall.data.model.Posting;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, UUID> {}
