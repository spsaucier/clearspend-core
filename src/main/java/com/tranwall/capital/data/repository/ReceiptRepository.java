package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, TypedId<ReceiptId>> {}
