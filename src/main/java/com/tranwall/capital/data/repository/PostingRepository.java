package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.PostingId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Posting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, TypedId<PostingId>> {}
