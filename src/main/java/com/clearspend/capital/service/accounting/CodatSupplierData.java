package com.clearspend.capital.service.accounting;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.text.similarity.FuzzyScore;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodatSupplierData {

  private String id;
  private String name;
  private Integer matchScore;

  public void calculateAndSetScore(String target) {
    FuzzyScore score = new FuzzyScore(Locale.ENGLISH);
    this.matchScore = score.fuzzyScore(target, name);
  }
}
