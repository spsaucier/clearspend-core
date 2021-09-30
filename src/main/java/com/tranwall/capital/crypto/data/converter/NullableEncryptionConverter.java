package com.tranwall.capital.crypto.data.converter;

import com.tranwall.capital.crypto.Crypto;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired, @Lazy})
@Converter
public class NullableEncryptionConverter implements AttributeConverter<String, byte[]> {

  private final Crypto crypto;

  @Override
  public byte[] convertToDatabaseColumn(String attribute) {
    return attribute == null ? null : crypto.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(byte[] dbData) {
    return dbData == null ? null : new String(crypto.decrypt(dbData));
  }
}
