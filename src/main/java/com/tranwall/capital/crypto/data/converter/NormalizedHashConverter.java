package com.tranwall.capital.crypto.data.converter;

import com.tranwall.capital.crypto.HashUtil;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired, @Lazy}))
@Converter
public class NormalizedHashConverter implements AttributeConverter<byte[], byte[]> {

  @Override
  public byte[] convertToDatabaseColumn(byte[] attribute) {
    return HashUtil.normalizedHash(new String(attribute)).toByteArray();
  }

  @Override
  public byte[] convertToEntityAttribute(byte[] dbData) {
    return dbData;
  }
}
