package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {}
