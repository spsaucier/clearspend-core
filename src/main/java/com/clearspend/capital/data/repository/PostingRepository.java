package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.PostingId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Posting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, TypedId<PostingId>> {}
