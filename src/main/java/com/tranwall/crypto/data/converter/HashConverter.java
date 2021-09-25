package com.tranwall.crypto.data.converter;

import com.tranwall.crypto.Crypto;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired, @Lazy}))
@Converter
public class HashConverter implements AttributeConverter<byte[], byte[]> {

  @NonNull @Lazy private final Crypto crypto;

  @Override
  public byte[] convertToDatabaseColumn(byte[] attribute) {
    return crypto.calculateHash(attribute);
  }

  @Override
  public byte[] convertToEntityAttribute(byte[] dbData) {
    return dbData;
  }
}
