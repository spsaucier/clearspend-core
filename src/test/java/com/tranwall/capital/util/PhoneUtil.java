package com.tranwall.capital.util;

import static java.util.stream.Collectors.joining;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public final class PhoneUtil {
  private PhoneUtil() {}

  // https://en.wikipedia.org/wiki/List_of_North_American_Numbering_Plan_area_codes#United_States
  private static final int[] areaCodes = {
    212,
    315,
    332,
    347,
    516,
    518,
    585,
    607,
    631,
    646,
    680,
    716,
    718,
    838,
    845,
    914,
    917,
    929,
    934,
    210,
    214,
    254,
    281,
    325,
    346,
    361,
    409,
    430,
    432,
    469,
    512,
    682,
    713,
    726,
    737,
    806,
    817,
    830,
    832,
    903,
    915,
    936,
    940,
    956,
    972,
    979
  };

  public static String randomPhoneNumber() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return String.format(
        "+1%d%d%s",
        areaCodes[random.nextInt(areaCodes.length)],
        random.nextInt(2, 10),
        IntStream.generate(() -> random.nextInt(10))
            .limit(6)
            .mapToObj(Integer::toString)
            .collect(joining()));
  }
}
