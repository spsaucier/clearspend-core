package com.clearspend.capital.data.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.CodatCategory;
import com.clearspend.capital.data.model.enums.CodatCategoryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CodatCategoryRepositoryTest extends BaseCapitalTest {

  @Autowired private CodatCategoryRepository codatCategoryRepository;
  @Autowired private TestHelper testHelper;

  @Test
  void save() {
    TestHelper.CreateBusinessRecord businessRecord = testHelper.createBusiness();
    CodatCategory codatCategory =
        new CodatCategory(
            businessRecord.business().getBusinessId(),
            "1",
            "Class Name",
            "Class Name",
            CodatCategoryType.CLASS);
    codatCategoryRepository.save(codatCategory);
    assertThat(codatCategoryRepository.findById(codatCategory.getId()).get())
        .isEqualTo(codatCategory);
  }
}
