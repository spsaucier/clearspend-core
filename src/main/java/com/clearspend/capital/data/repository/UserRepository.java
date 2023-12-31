package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<User, TypedId<UserId>>,
        JpaSpecificationExecutor<User>,
        UserRepositoryCustom {

  List<User> findByBusinessId(TypedId<BusinessId> businessId);

  Optional<User> findBySubjectRef(String subjectRef);

  Optional<User> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<UserId> id);

  Page<User> findAllByBusinessId(final TypedId<BusinessId> businessId, final Pageable pageable);

  List<User> findByBusinessIdAndFirstNameLikeOrLastNameLike(
      TypedId<BusinessId> businessId,
      RequiredEncryptedStringWithHash userFirstName,
      RequiredEncryptedStringWithHash userLastName);

  List<User> findByBusinessIdAndIdIn(TypedId<BusinessId> businessId, List<TypedId<UserId>> ids);

  List<User> findAllByIdIn(final Collection<TypedId<UserId>> ids);

  Optional<User> findByEmailHash(byte[] emailHash);
}
