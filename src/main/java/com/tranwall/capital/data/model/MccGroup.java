package com.tranwall.capital.data.model;

import com.google.common.collect.Range;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.MccGroupId;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
public class MccGroup extends TypedMutable<MccGroupId> {

  @NonNull private String name;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private List<Range<String>> mccCodes;
}
