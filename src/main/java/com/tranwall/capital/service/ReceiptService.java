package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.Receipt;
import com.tranwall.capital.data.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

  private final ReceiptRepository receiptRepository;

  private final AdjustmentService adjustmentService;

  public Receipt createReceipt(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount,
      MultipartFile receiptFile) {
    Adjustment adjustment = adjustmentService.retrieveAdjustment(adjustmentId);
    return receiptRepository.save(
        new Receipt(businessId, adjustment.getAccountId(), adjustmentId, amount));
  }
}
