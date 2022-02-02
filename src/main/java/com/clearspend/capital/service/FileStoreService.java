package com.clearspend.capital.service;

import com.clearspend.capital.client.gcs.GoogleCloudStorageClient;
import com.clearspend.capital.common.typedid.data.FileStoreId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.data.model.FileStore;
import com.clearspend.capital.data.repository.FileStoreRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStoreService {

  private final GoogleCloudStorageClient googleCloudStorageClient;
  private final FileStoreRepository fileStoreRepository;

  public FileStore saveFileForBusiness(
      TypedId<BusinessId> businessId, String fileName, String purpose, MultipartFile file)
      throws IOException {

    FileStore fileStore = new FileStore(businessId);
    fileStore.setFileName(fileName);
    fileStore.setPurpose(purpose);
    fileStoreRepository.save(fileStore);

    String fileStorePath = getFileStorePath(businessId, fileStore.getId());
    fileStore.setPath(fileStorePath);

    googleCloudStorageClient.writeOnboardFile(fileStorePath, file.getBytes());
    return fileStore;
  }

  public FileStore saveFileForBusinessOwner(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      String fileName,
      String purpose,
      MultipartFile file)
      throws IOException {

    FileStore fileStore = new FileStore(businessId);
    fileStore.setBusinessOwnerId(businessOwnerId);
    fileStore.setFileName(fileName);
    fileStore.setPurpose(purpose);
    fileStore.setPath(getFileStorePath(businessId, businessOwnerId, fileStore.getId()));
    fileStoreRepository.save(fileStore);

    String fileStorePath = getFileStorePath(businessId, businessOwnerId, fileStore.getId());
    fileStore.setPath(fileStorePath);

    googleCloudStorageClient.writeOnboardFile(fileStorePath, file.getBytes());
    return fileStore;
  }

  public byte[] getFileStoreData(String fileStorePath) {
    return googleCloudStorageClient.readOnboardFile(fileStorePath);
  }

  private String getFileStorePath(
      TypedId<BusinessId> businessId,
      TypedId<BusinessOwnerId> businessOwnerId,
      TypedId<FileStoreId> fileStoreId) {
    return String.format(
        "/fileStore/bid_%s/oid_%s/fid_%s", businessId, businessOwnerId, fileStoreId);
  }

  private String getFileStorePath(
      TypedId<BusinessId> businessId, TypedId<FileStoreId> fileStoreId) {
    return String.format("/fileStore/bid_%s/fid_%s", businessId, fileStoreId);
  }
}
