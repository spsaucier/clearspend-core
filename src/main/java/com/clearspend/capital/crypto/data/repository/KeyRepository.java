package com.clearspend.capital.crypto.data.repository;

import com.clearspend.capital.crypto.data.model.Key;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface KeyRepository extends Repository<Key, UUID> {
  List<Key> findAll();

  <S extends Key> S save(S entity);
}
