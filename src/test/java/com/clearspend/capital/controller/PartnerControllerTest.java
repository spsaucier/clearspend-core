package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.controller.type.partner.PartnerBusiness;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.PartnerService;
import com.clearspend.capital.service.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PartnerControllerTest {

  private PartnerService mockPartnerService;
  private UserService mockUserService;

  private PartnerController underTest;

  @BeforeEach
  public void setup() {
    mockPartnerService = mock(PartnerService.class);
    mockUserService = mock(UserService.class);

    underTest = new PartnerController(mockPartnerService, mockUserService);
  }

  @Test
  @SneakyThrows
  void getAllPartnerBusinesses_whenUserCannotBeFound_RecordNotFoundExceptionIsThrown() {
    // GIVEN
    when(mockUserService.retrieveUser(any()))
        .thenThrow(new RecordNotFoundException(Table.USER, "123"));

    // WHEN & THEN
    assertThrows(RecordNotFoundException.class, () -> underTest.getAllPartnerBusinesses());
  }

  @Test
  @SneakyThrows
  void getAllPartnerBusinesses_whenUserIsFound_partnerServiceResultIsReturned() {
    // GIVEN
    List<PartnerBusiness> businesses = new ArrayList<>();
    when(mockUserService.retrieveUser(any())).thenReturn(new User());
    when(mockPartnerService.getAllPartneredBusinessesForUser(any())).thenReturn(businesses);

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPartnerBusinesses();

    // THEN
    assertThat(result).isNotNull().isSameAs(businesses);
  }

  @Test
  @SneakyThrows
  void getPinnedBusinesses_whenUserCannotBeFound_RecordNotFoundExceptionIsThrown() {
    // GIVEN
    when(mockUserService.retrieveUser(any()))
        .thenThrow(new RecordNotFoundException(Table.USER, "123"));

    // WHEN & THEN
    assertThrows(RecordNotFoundException.class, () -> underTest.getPinnedBusinesses());
  }

  @Test
  @SneakyThrows
  void getPinnedBusinesses_whenUserIsFound_partnerServiceResultIsReturned() {
    // GIVEN
    List<PartnerBusiness> businesses = new ArrayList<>();
    when(mockUserService.retrieveUser(any())).thenReturn(new User());
    when(mockPartnerService.getAllPinnedBusinessesForUser(any())).thenReturn(businesses);

    // WHEN
    List<PartnerBusiness> result = underTest.getPinnedBusinesses();

    // THEN
    assertThat(result).isNotNull();
  }
}
