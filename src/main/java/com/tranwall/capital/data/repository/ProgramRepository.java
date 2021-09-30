package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Program;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, UUID> {}
