package com.clearspend.capital.crypto.data.converter;

import com.clearspend.capital.crypto.data.ByteString;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public final class ByteStringConverter implements AttributeConverter<ByteString, byte[]> {

  @Override
  public byte[] convertToDatabaseColumn(ByteString attribute) {
    return attribute == null ? null : attribute.toByteArray();
  }

  @Override
  public ByteString convertToEntityAttribute(byte[] dbData) {
    return dbData == null ? null : ByteString.of(dbData);
  }
}
