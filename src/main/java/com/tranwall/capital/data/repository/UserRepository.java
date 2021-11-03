package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, TypedId<UserId>> {

  List<User> findByBusinessId(TypedId<BusinessId> businessId);

  Optional<User> findBySubjectRef(String subjectRef);

  List<User> findByBusinessIdAndFirstNameLikeOrLastNameLike(
      TypedId<BusinessId> businessId,
      RequiredEncryptedStringWithHash userFirstName,
      RequiredEncryptedStringWithHash userLastName);
}
