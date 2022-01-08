package com.clearspend.capital.data.repository.ledger;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.ledger.PostingId;
import com.clearspend.capital.data.model.ledger.Posting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, TypedId<PostingId>> {}
