package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Program;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, TypedId<ProgramId>> {

  List<Program> findProgramsByIdIn(Set<TypedId<ProgramId>> programIds);
}
