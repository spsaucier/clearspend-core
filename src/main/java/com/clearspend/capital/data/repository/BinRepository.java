package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BinId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Bin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinRepository extends JpaRepository<Bin, TypedId<BinId>> {

  Optional<Bin> findByBin(String bin);
}
