package com.tranwall.capital.common.typedid.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tranwall.capital.common.typedid.codec.TypedIdDeserializer;
import com.tranwall.capital.common.typedid.codec.TypedIdSerializer;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
@JsonSerialize(using = TypedIdSerializer.class)
@JsonDeserialize(using = TypedIdDeserializer.class)
public final class TypedId<E> implements Serializable {

  @Serial private static final long serialVersionUID = 7945248460416145026L;

  @NonNull private final UUID uuid;

  public TypedId() {
    this(UUID.randomUUID());
  }

  public TypedId(@NonNull UUID uuid) {
    this.uuid = uuid;
  }

  public TypedId(@NonNull String uuid) {
    this(UUID.fromString(uuid));
  }

  @Override
  public String toString() {
    return uuid.toString();
  }

  @NonNull
  public UUID toUuid() {
    return uuid;
  }
}
