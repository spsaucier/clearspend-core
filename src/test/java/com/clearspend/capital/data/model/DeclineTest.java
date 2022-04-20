package com.clearspend.capital.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.decline.AddressPostalCodeMismatch;
import com.clearspend.capital.data.model.decline.Decline;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.decline.LimitExceeded;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.repository.DeclineRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class DeclineTest extends BaseCapitalTest {
  private final DeclineRepository declineRepository;
  private final TestHelper testHelper;

  private CreateBusinessRecord createBusinessRecord;
  private Card card;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.INDIVIDUAL,
            CardType.VIRTUAL,
            false);
  }

  @Test
  void savesAndLoadsDecline_SerializesDetailsProperly() {
    final AddressPostalCodeMismatch mismatch = new AddressPostalCodeMismatch("12345");
    final DeclineDetails declineDetails = new DeclineDetails(DeclineReason.INSUFFICIENT_FUNDS);
    final LimitExceeded limitExceeded =
        new LimitExceeded(
            UUID.randomUUID(),
            TransactionLimitType.ALLOCATION,
            LimitType.PURCHASE,
            LimitPeriod.DAILY,
            new BigDecimal("10"));
    final List<DeclineDetails> list = List.of(mismatch, declineDetails, limitExceeded);

    final Amount amount = new Amount(Currency.USD, new BigDecimal("10"));

    final Decline decline =
        declineRepository.save(
            new Decline(
                createBusinessRecord.business().getId(),
                createBusinessRecord.allocationRecord().account().getId(),
                card.getId(),
                new Amount(Currency.USD, new BigDecimal(10)).toAmount(),
                list));

    final Decline result = declineRepository.findById(decline.getId()).orElseThrow();
    assertEquals(decline, result);
    // This should be evaluated by the above assertEquals(), but given the issues here I just want
    // to be absolutely, 100% sure it's right
    assertEquals(list, result.getDetails());
  }
}
