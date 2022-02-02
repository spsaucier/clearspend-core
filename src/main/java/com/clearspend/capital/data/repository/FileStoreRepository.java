package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.FileStoreId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.data.model.FileStore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileStoreRepository extends JpaRepository<FileStore, TypedId<FileStoreId>> {

  Optional<FileStore> findFileStoreByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<FileStoreId> id);

  List<FileStore> findFileStoreByBusinessIdAndBusinessOwnerId(
      TypedId<BusinessId> businessId, TypedId<BusinessOwnerId> businessOwnerId);
}
