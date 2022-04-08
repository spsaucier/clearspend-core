package com.clearspend.capital.util.function;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.crypto.Crypto;
import java.sql.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;

public interface TypeFunctions {
  static UUID nullableTypedIdToUUID(@Nullable final TypedId<?> typedId) {
    return Optional.ofNullable(typedId).map(TypedId::toUuid).orElse(null);
  }

  static <T> T[] nullableCollectionToNonNullArray(
      @Nullable final Collection<T> collection, @NonNull IntFunction<T[]> generator) {
    return Optional.ofNullable(collection)
        .map(col -> col.toArray(generator))
        .orElse(generator.apply(0));
  }

  static <T extends Enum<T>> T nullableStringToEnum(
      @Nullable final String value, @NonNull final Function<String, T> valueOf) {
    return Optional.ofNullable(value).map(valueOf).orElse(null);
  }

  static <T> TypedId<T> nullableUuidToTypedId(@Nullable final UUID uuid) {
    return Optional.ofNullable(uuid).map(id -> new TypedId<T>(id)).orElse(null);
  }

  static <T extends Enum<T>> List<String> nullableEnumListToStringList(
      @Nullable final List<T> enumList) {
    return Optional.ofNullable(enumList)
        .map(list -> list.stream().map(Enum::name).toList())
        .orElse(null);
  }

  static <T> List<UUID> nullableTypedIdListToUuidList(
      @Nullable final List<TypedId<T>> typedIdList) {
    return Optional.ofNullable(typedIdList)
        .map(list -> list.stream().map(TypedId::toUuid).toList())
        .orElse(null);
  }

  static Stream<Object> nullableSqlArrayToStream(@Nullable Array array) {
    return Optional.ofNullable(array)
        .map(ThrowableFunctions.sneakyThrows(Array::getArray))
        .map(arr -> (Object[]) arr)
        .stream()
        .flatMap(Arrays::stream);
  }

  static <T extends Enum<T>> EnumSet<T> nullableSqlArrayToEnumSet(
      @Nullable Array array,
      @NonNull final Class<T> enumType,
      @NonNull final Function<String, T> valueOf) {
    return nullableSqlArrayToStream(array)
        .map(Objects::requireNonNull)
        .map(String::valueOf)
        .map(valueOf)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(enumType)));
  }

  static String nullableBytesToDecryptedString(
      @Nullable final byte[] bytes, @NonNull final Crypto crypto) {
    return Optional.ofNullable(bytes).map(crypto::decrypt).map(String::new).orElse(null);
  }
}
