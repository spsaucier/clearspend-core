package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<User, TypedId<UserId>>,
        JpaSpecificationExecutor<User>,
        UserRepositoryCustom {

  List<User> findByBusinessId(TypedId<BusinessId> businessId);

  Optional<User> findBySubjectRef(String subjectRef);

  Optional<User> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<UserId> id);

  List<User> findByBusinessIdAndFirstNameLikeOrLastNameLike(
      TypedId<BusinessId> businessId,
      RequiredEncryptedStringWithHash userFirstName,
      RequiredEncryptedStringWithHash userLastName);

  List<User> findByBusinessIdAndIdIn(TypedId<BusinessId> businessId, List<TypedId<UserId>> ids);
}
