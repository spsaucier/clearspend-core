package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ProgramId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Program;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, TypedId<ProgramId>> {

  List<Program> findProgramsByIdIn(Set<TypedId<ProgramId>> programIds);
}
