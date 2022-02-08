package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.data.model.FileStore;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.repository.FileStoreRepository;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class FileStoreServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private FileStoreService fileStoreService;
  @Autowired private FileStoreRepository fileStoreRepository;

  @SneakyThrows
  @Test
  void saveFileForBusiness_success() {
    Business business = testHelper.createBusiness().business();
    FileStore fileStore =
        fileStoreService.saveFileForBusiness(
            business.getId(), "fileName", "Identity", "Hello".getBytes());
    FileStore fileStoreByBusinessIdAndId =
        fileStoreRepository
            .findFileStoreByBusinessIdAndId(business.getId(), fileStore.getId())
            .orElseThrow(() -> new RecordNotFoundException(Table.FILE_STORE, fileStore.getId()));
    Assertions.assertEquals(
        "Hello",
        new String(fileStoreService.getFileStoreData(fileStoreByBusinessIdAndId.getPath())));
  }

  @SneakyThrows
  @Test
  void saveFileForBusinessOwner_success() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    BusinessOwner businessOwner = createBusinessRecord.businessOwner();
    fileStoreService.saveFileForBusinessOwner(
        business.getId(), businessOwner.getId(), "fileName", "Identity", "Hello".getBytes());
    List<FileStore> fileStoreByBusinessIdAndId =
        fileStoreRepository.findFileStoreByBusinessIdAndBusinessOwnerId(
            business.getId(), businessOwner.getId());
    Assertions.assertEquals(
        "Hello",
        new String(fileStoreService.getFileStoreData(fileStoreByBusinessIdAndId.get(0).getPath())));
  }
}
