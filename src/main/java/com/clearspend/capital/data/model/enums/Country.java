package com.clearspend.capital.data.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// from: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3#Officially_assigned_code_elements
// included all countries to handle the conversion of string to enum for i2c transactions
@Slf4j
public enum Country {
  UNSPECIFIED(""),
  ABW("AW"), // Aruba
  AFG("AF"), // Afghanistan
  AGO("AO"), // Angola
  AIA("AI"), // Anguilla
  ALA("AX"), // Aland Islands
  ALB("AL"), // Albania
  AND("AD"), // Andorra
  ANT("AN"), // Netherlands Antilles
  ARE("AE"), // United Arab Emirates
  ARG("AR"), // Argentina
  ARM("AM"), // Armenia
  ASM("AS"), // American Samoa
  ATA("AQ"), // Antarctica
  ATF("TF"), // French Southern Territories
  ATG("AG"), // Antigua and Barbuda
  AUS("AU"), // Australia
  AUT("AT"), // Austria
  AZE("AZ"), // Azerbaijan
  BDI("BI"), // Burundi
  BEL("BE"), // Belgium
  BEN("BJ"), // Benin
  BFA("BF"), // Burkina Faso
  BGD("BD"), // Bangladesh
  BGR("BG"), // Bulgaria
  BHR("BH"), // Bahrain
  BHS("BS"), // Bahamas
  BIH("BA"), // Bosnia and Herzegovina
  BLM("BL"), // Saint-Barthélemy
  BLR("BY"), // Belarus
  BLZ("BZ"), // Belize
  BMU("BM"), // Bermuda
  BOL("BO"), // Bolivia
  BRA("BR"), // Brazil
  BRB("BB"), // Barbados
  BRN("BN"), // Brunei Darussalam
  BTN("BT"), // Bhutan
  BVT("BV"), // Bouvet Island
  BWA("BW"), // Botswana
  CAF("CF"), // Central African Republic
  CAN("CA"), // Canada
  CCK("CC"), // Cocos (Keeling) Islands
  CHE("CH"), // Switzerland
  CHL("CL"), // Chile
  CHN("CN"), // China
  CIV("CI"), // Côte d'Ivoire
  CMR("CM"), // Cameroon
  COD("CD"), // Congo, (Kinshasa)
  COG("CG"), // Congo (Brazzaville)
  COK("CK"), // Cook Islands
  COL("CO"), // Colombia
  COM("KM"), // Comoros
  CPV("CV"), // Cape Verde
  CRI("CR"), // Costa Rica
  CUB("CU"), // Cuba
  CXR("CX"), // Christmas Island
  CYM("KY"), // Cayman Islands
  CYP("CY"), // Cyprus
  CZE("CZ"), // Czech Republic
  DEU("DE"), // Germany
  DJI("DJ"), // Djibouti
  DMA("DM"), // Dominica
  DNK("DK"), // Denmark
  DOM("DO"), // Dominican Republic
  DZA("DZ"), // Algeria
  ECU("EC"), // Ecuador
  EGY("EG"), // Egypt
  ERI("ER"), // Eritrea
  ESH("EH"), // Western Sahara
  ESP("ES"), // Spain
  EST("EE"), // Estonia
  ETH("ET"), // Ethiopia
  FIN("FI"), // Finland
  FJI("FJ"), // Fiji
  FLK("FK"), // Falkland Islands (Malvinas)
  FRA("FR"), // France
  FRO("FO"), // Faroe Islands
  FSM("FM"), // Micronesia, Federated States of
  GAB("GA"), // Gabon
  GBR("GB"), // United Kingdom
  GEO("GE"), // Georgia
  GGY("GG"), // Guernsey
  GHA("GH"), // Ghana
  GIB("GI"), // Gibraltar
  GIN("GN"), // Guinea
  GLP("GP"), // Guadeloupe
  GMB("GM"), // Gambia
  GNB("GW"), // Guinea-Bissau
  GNQ("GQ"), // Equatorial Guinea
  GRC("GR"), // Greece
  GRD("GD"), // Grenada
  GRL("GL"), // Greenland
  GTM("GT"), // Guatemala
  GUF("GF"), // French Guiana
  GUM("GU"), // Guam
  GUY("GY"), // Guyana
  HKG("HK"), // Hong Kong, SAR China
  HMD("HM"), // Heard and Mcdonald Islands
  HND("HN"), // Honduras
  HRV("HR"), // Croatia
  HTI("HT"), // Haiti
  HUN("HU"), // Hungary
  IDN("ID"), // Indonesia
  IMN("IM"), // Isle of Man
  IND("IN"), // India
  IOT("IO"), // British Indian Ocean Territory
  IRL("IE"), // Ireland
  IRN("IR"), // Iran, Islamic Republic of
  IRQ("IQ"), // Iraq
  ISL("IS"), // Iceland
  ISR("IL"), // Israel
  ITA("IT"), // Italy
  JAM("JM"), // Jamaica
  JEY("JE"), // Jersey
  JOR("JO"), // Jordan
  JPN("JP"), // Japan
  KAZ("KZ"), // Kazakhstan
  KEN("KE"), // Kenya
  KGZ("KG"), // Kyrgyzstan
  KHM("KH"), // Cambodia
  KIR("KI"), // Kiribati
  KNA("KN"), // Saint Kitts and Nevis
  KOR("KR"), // Korea (South)
  KWT("KW"), // Kuwait
  LAO("LA"), // Lao PDR
  LBN("LB"), // Lebanon
  LBR("LR"), // Liberia
  LBY("LY"), // Libya
  LCA("LC"), // Saint Lucia
  LIE("LI"), // Liechtenstein
  LKA("LK"), // Sri Lanka
  LSO("LS"), // Lesotho
  LTU("LT"), // Lithuania
  LUX("LU"), // Luxembourg
  LVA("LV"), // Latvia
  MAC("MO"), // Macao, SAR China
  MAF("MF"), // Saint-Martin (French part)
  MAR("MA"), // Morocco
  MCO("MC"), // Monaco
  MDA("MD"), // Moldova
  MDG("MG"), // Madagascar
  MDV("MV"), // Maldives
  MEX("MX"), // Mexico
  MHL("MH"), // Marshall Islands
  MKD("MK"), // Macedonia, Republic of
  MLI("ML"), // Mali
  MLT("MT"), // Malta
  MMR("MM"), // Myanmar
  MNE("ME"), // Montenegro
  MNG("MN"), // Mongolia
  MNP("MP"), // Northern Mariana Islands
  MOZ("MZ"), // Mozambique
  MRT("MR"), // Mauritania
  MSR("MS"), // Montserrat
  MTQ("MQ"), // Martinique
  MUS("MU"), // Mauritius
  MWI("MW"), // Malawi
  MYS("MY"), // Malaysia
  MYT("YT"), // Mayotte
  NAM("NA"), // Namibia
  NCL("NC"), // New Caledonia
  NER("NE"), // Niger
  NFK("NF"), // Norfolk Island
  NGA("NG"), // Nigeria
  NIC("NI"), // Nicaragua
  NIU("NU"), // Niue
  NLD("NL"), // Netherlands
  NOR("NO"), // Norway
  NPL("NP"), // Nepal
  NRU("NR"), // Nauru
  NZL("NZ"), // New Zealand
  OMN("OM"), // Oman
  PAK("PK"), // Pakistan
  PAN("PA"), // Panama
  PCN("PN"), // Pitcairn
  PER("PE"), // Peru
  PHL("PH"), // Philippines
  PLW("PW"), // Palau
  PNG("PG"), // Papua New Guinea
  POL("PL"), // Poland
  PRI("PR"), // Puerto Rico
  PRK("KP"), // Korea (North)
  PRT("PT"), // Portugal
  PRY("PY"), // Paraguay
  PSE("PS"), // Palestinian Territory
  PYF("PF"), // French Polynesia
  QAT("QA"), // Qatar
  REU("RE"), // Réunion
  ROU("RO"), // Romania
  RUS("RU"), // Russian Federation
  RWA("RW"), // Rwanda
  SAU("SA"), // Saudi Arabia
  SDN("SD"), // Sudan
  SEN("SN"), // Senegal
  SGP("SG"), // Singapore
  SGS("GS"), // South Georgia and the South Sandwich Islands
  SHN("SH"), // Saint Helena
  SJM("SJ"), // Svalbard and Jan Mayen Islands
  SLB("SB"), // Solomon Islands
  SLE("SL"), // Sierra Leone
  SLV("SV"), // El Salvador
  SMR("SM"), // San Marino
  SOM("SO"), // Somalia
  SPM("PM"), // Saint Pierre and Miquelon
  SRB("RS"), // Serbia
  SSD("SS"), // South Sudan
  STP("ST"), // Sao Tome and Principe
  SUR("SR"), // Suriname
  SVK("SK"), // Slovakia
  SVN("SI"), // Slovenia
  SWE("SE"), // Sweden
  SWZ("SZ"), // Swaziland
  SYC("SC"), // Seychelles
  SYR("SY"), // Syrian Arab Republic (Syria)
  TCA("TC"), // Turks and Caicos Islands
  TCD("TD"), // Chad
  TGO("TG"), // Togo
  THA("TH"), // Thailand
  TJK("TJ"), // Tajikistan
  TKL("TK"), // Tokelau
  TKM("TM"), // Turkmenistan
  TLS("TL"), // Timor-Leste
  TON("TO"), // Tonga
  TTO("TT"), // Trinidad and Tobago
  TUN("TN"), // Tunisia
  TUR("TR"), // Turkey
  TUV("TV"), // Tuvalu
  TWN("TW"), // Taiwan, Republic of China
  TZA("TZ"), // Tanzania, United Republic of
  UGA("UG"), // Uganda
  UKR("UA"), // Ukraine
  UMI("UM"), // US Minor Outlying Islands
  URY("UY"), // Uruguay
  USA("US"), // United States of America
  UZB("UZ"), // Uzbekistan
  VAT("VA"), // Holy See (Vatican City State)
  VCT("VC"), // Saint Vincent and Grenadines
  VEN("VE"), // Venezuela (Bolivarian Republic)
  VGB("VG"), // British Virgin Islands
  VIR("VI"), // Virgin Islands, US
  VNM("VN"), // Viet Nam
  VUT("VU"), // Vanuatu
  WLF("WF"), // Wallis and Futuna Islands
  WSM("WS"), // Samoa
  YEM("YE"), // Yemen
  ZAF("ZA"), // South Africa
  ZMB("ZM"), // Zambia
  ZWE("ZW"), // Zimbabwe
  ;

  @Getter private final String twoCharacterCode;

  private static Map<String, Country> map = initializeMap();

  private static Map<String, Country> initializeMap() {
    return Arrays.stream(Country.values())
        .collect(Collectors.toUnmodifiableMap(e -> e.twoCharacterCode, Function.identity()));
  }

  Country(String twoCharacterCode) {
    this.twoCharacterCode = twoCharacterCode;
  }

  public static Country of(String country) {
    if (country == null) {
      return UNSPECIFIED;
    }

    return switch (country.trim().length()) {
      case 2 -> map.get(country.trim().toUpperCase());
      case 3 -> valueOf(country.trim().toUpperCase());
      default -> UNSPECIFIED;
    };
  }
}
