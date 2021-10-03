package com.tranwall.capital.common.masking;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class SensitiveFieldSerializer extends JsonSerializer<Object> {

  @Value("${tranwall.logging.mask.disable:false}")
  private boolean disableMasking;

  public SensitiveFieldSerializer() {
    super();
  }

  @Override
  public void serialize(Object value, JsonGenerator jsonGenerator, SerializerProvider provider)
      throws IOException {
    jsonGenerator.writeString(mask(disableMasking, value));
  }

  @Override
  public void serializeWithType(
      Object value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
      throws IOException {
    gen.writeString(mask(disableMasking, value));
  }

  // replace numbers with #, letters with a
  public static String mask(boolean disableMasking, Object objectValue) {
    if (objectValue == null || objectValue.toString() == null) {
      return null;
    }

    if (disableMasking) {
      return objectValue.toString();
    }

    // custom logic to support adding @Mask to Map<String, String>
    if (objectValue instanceof Map) {
      Map map = new HashMap((Map) objectValue);
      for (Object x : map.keySet()) {
        map.put(x, maskString(map.get(x)));
      }

      return new Gson().toJson(map);
    }

    return maskString(objectValue);
  }

  private static String maskString(Object objectValue) {
    if (objectValue == null) {
      return null;
    }

    // below values allow for digits, then unicode values vs ansi
    String result =
        objectValue
            .toString()
            .replaceAll("[0-9]", "#")
            .replaceAll("\\p{Lu}", "A")
            .replaceAll("\\p{Ll}", "a");

    if (result.length() > 50) {
      return String.format(
          "%s<%04d>%s",
          result.substring(0, 25), result.length() - 50, result.substring(result.length() - 25));
    }

    return result;
  }

  public static String maskEmail(boolean disableMasking, String email) {
    if (email == null || disableMasking) {
      return email;
    }

    int idx = email.indexOf("@");
    if (idx == -1) {
      return mask(disableMasking, email);
    }

    String name = email.substring(0, idx);
    String atDomain = email.substring(idx);
    if (name.length() > 4) {
      name =
          name.substring(0, 2)
              + StringUtils.repeat(".", name.length() - 4)
              + name.substring(name.length() - 2);
    }

    return name + atDomain;
  }

  public static String maskPhone(boolean disableMasking, String phone) {
    return phone == null || disableMasking || phone.length() < 6
        ? phone
        : phone.substring(0, 3)
            + mask(disableMasking, phone.substring(3, phone.length() - 2))
            + phone.substring(phone.length() - 2);
  }

  public static String maskAccessToken(boolean disableMasking, String accessToken) {
    if (accessToken == null || disableMasking) {
      return accessToken;
    }

    String[] segments = accessToken.split("\\.");
    if (segments.length != 3) {
      return mask(disableMasking, accessToken);
    }

    return String.join(".", segments[0], segments[1], mask(disableMasking, segments[1]));
  }

  public static String maskTaxId(String taxId) {
    return maskX(taxId);
  }

  public static String maskDateOfBirth(String dateOfBirth) {
    return maskX(dateOfBirth);
  }

  public static String maskAccountNumber(String accountNumber) {
    if (accountNumber != null) {
      return maskExceptLastFourDigit(accountNumber);
    }
    return accountNumber;
  }

  public static String maskPinNumber(String accountPin) {
    if (accountPin != null) {
      return maskX(accountPin);
    }
    return accountPin;
  }

  public static String maskBarcodeNumber(String barcodeNumber) {
    if (barcodeNumber != null) {
      return maskExceptLastFourDigit(barcodeNumber);
    }
    return barcodeNumber;
  }

  private static String maskExceptLastFourDigit(String value) {
    int length = value.length();
    if (length > 4) {
      return maskX(value.substring(0, length - 4)) + value.substring(length - 4);
    }
    return value;
  }

  private static String maskX(String objectValue) {
    if (objectValue == null) {
      return null;
    }
    String fieldValue = objectValue.toString();
    String result = fieldValue.replaceAll("[0-9 A-Z a-z]", "X");
    if (result.length() > 50) {
      return String.format(
          "%s<%04d>%s",
          result.substring(0, 25), result.length() - 50, result.substring(result.length() - 25));
    }
    return result;
  }
}
