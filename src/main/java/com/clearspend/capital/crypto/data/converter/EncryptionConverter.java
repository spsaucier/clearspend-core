package com.clearspend.capital.crypto.data.converter;

import com.clearspend.capital.crypto.Crypto;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired, @Lazy}))
@Converter
public class EncryptionConverter implements AttributeConverter<String, byte[]> {
  private final Crypto crypto;

  @Override
  public byte[] convertToDatabaseColumn(String attribute) {
    return crypto.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(byte[] dbData) {
    if (dbData == null) {
      return "";
    }

    return new String(crypto.decrypt(dbData));
  }
}
