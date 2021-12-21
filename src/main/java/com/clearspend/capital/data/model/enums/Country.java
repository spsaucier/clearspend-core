package com.clearspend.capital.data.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

// from: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3#Officially_assigned_code_elements
// included all countries to handle the conversion of string to enum for i2c transactions
@Slf4j
public enum Country {
  UNSPECIFIED(""),
  ABW("AD"), // Aruba
  AFG("AE"), // Afghanistan
  AGO("AF"), // Angola
  AIA("AG"), // Anguilla
  ALA("AI"), // Åland Islands
  ALB("AL"), // Albania
  AND("AM"), // Andorra
  ARE("AO"), // United Arab Emirates
  ARG("AQ"), // Argentina
  ARM("AR"), // Armenia
  ASM("AS"), // American Samoa
  ATA("AT"), // Antarctica
  ATF("AU"), // French Southern Territories
  ATG("AW"), // Antigua and Barbuda
  AUS("AX"), // Australia
  AUT("AZ"), // Austria
  AZE("BA"), // Azerbaijan
  BDI("BB"), // Burundi
  BEL("BD"), // Belgium
  BEN("BE"), // Benin
  BES("BF"), // Bonaire, Sint Eustatius and Saba
  BFA("BG"), // Burkina Faso
  BGD("BH"), // Bangladesh
  BGR("BI"), // Bulgaria
  BHR("BJ"), // Bahrain
  BHS("BL"), // Bahamas
  BIH("BM"), // Bosnia and Herzegovina
  BLM("BN"), // Saint Barthélemy
  BLR("BO"), // Belarus
  BLZ("BQ"), // Belize
  BMU("BR"), // Bermuda
  BOL("BS"), // Bolivia (Plurinational State of)
  BRA("BT"), // Brazil
  BRB("BV"), // Barbados
  BRN("BW"), // Brunei Darussalam
  BTN("BY"), // Bhutan
  BVT("BZ"), // Bouvet Island
  BWA("CA"), // Botswana
  CAF("CC"), // Central African Republic
  CAN("CD"), // Canada
  CCK("CF"), // Cocos (Keeling) Islands
  CHE("CG"), // Switzerland
  CHL("CH"), // Chile
  CHN("CI"), // China
  CIV("CK"), // Côte d'Ivoire
  CMR("CL"), // Cameroon
  COD("CM"), // Congo, Democratic Republic of the
  COG("CN"), // Congo
  COK("CO"), // Cook Islands
  COL("CR"), // Colombia
  COM("CU"), // Comoros
  CPV("CV"), // Cabo Verde
  CRI("CW"), // Costa Rica
  CUB("CX"), // Cuba
  CUW("CY"), // Curaçao
  CXR("CZ"), // Christmas Island
  CYM("DE"), // Cayman Islands
  CYP("DJ"), // Cyprus
  CZE("DK"), // Czechia
  DEU("DM"), // Germany
  DJI("DO"), // Djibouti
  DMA("DZ"), // Dominica
  DNK("EC"), // Denmark
  DOM("EE"), // Dominican Republic
  DZA("EG"), // Algeria
  ECU("EH"), // Ecuador
  EGY("ER"), // Egypt
  ERI("ES"), // Eritrea
  ESH("ET"), // Western Sahara
  ESP("FI"), // Spain
  EST("FJ"), // Estonia
  ETH("FK"), // Ethiopia
  FIN("FM"), // Finland
  FJI("FO"), // Fiji
  FLK("FR"), // Falkland Islands (Malvinas)
  FRA("GA"), // France
  FRO("GB"), // Faroe Islands
  FSM("GD"), // Micronesia (Federated States of)
  GAB("GE"), // Gabon
  GBR("GF"), // United Kingdom of Great Britain and Northern Ireland
  GEO("GG"), // Georgia
  GGY("GH"), // Guernsey
  GHA("GI"), // Ghana
  GIB("GL"), // Gibraltar
  GIN("GM"), // Guinea
  GLP("GN"), // Guadeloupe
  GMB("GP"), // Gambia
  GNB("GQ"), // Guinea-Bissau
  GNQ("GR"), // Equatorial Guinea
  GRC("GS"), // Greece
  GRD("GT"), // Grenada
  GRL("GU"), // Greenland
  GTM("GW"), // Guatemala
  GUF("GY"), // French Guiana
  GUM("HK"), // Guam
  GUY("HM"), // Guyana
  HKG("HN"), // Hong Kong
  HMD("HR"), // Heard Island and McDonald Islands
  HND("HT"), // Honduras
  HRV("HU"), // Croatia
  HTI("ID"), // Haiti
  HUN("IE"), // Hungary
  IDN("IL"), // Indonesia
  IMN("IM"), // Isle of Man
  IND("IN"), // India
  IOT("IO"), // British Indian Ocean Territory
  IRL("IQ"), // Ireland
  IRN("IR"), // Iran (Islamic Republic of)
  IRQ("IS"), // Iraq
  ISL("IT"), // Iceland
  ISR("JE"), // Israel
  ITA("JM"), // Italy
  JAM("JO"), // Jamaica
  JEY("JP"), // Jersey
  JOR("KE"), // Jordan
  JPN("KG"), // Japan
  KAZ("KH"), // Kazakhstan
  KEN("KI"), // Kenya
  KGZ("KM"), // Kyrgyzstan
  KHM("KN"), // Cambodia
  KIR("KP"), // Kiribati
  KNA("KR"), // Saint Kitts and Nevis
  KOR("KW"), // Korea, Republic of
  KWT("KY"), // Kuwait
  LAO("KZ"), // Lao People's Democratic Republic
  LBN("LA"), // Lebanon
  LBR("LB"), // Liberia
  LBY("LC"), // Libya
  LCA("LI"), // Saint Lucia
  LIE("LK"), // Liechtenstein
  LKA("LR"), // Sri Lanka
  LSO("LS"), // Lesotho
  LTU("LT"), // Lithuania
  LUX("LU"), // Luxembourg
  LVA("LV"), // Latvia
  MAC("LY"), // Macao
  MAF("MA"), // Saint Martin (French part)
  MAR("MC"), // Morocco
  MCO("MD"), // Monaco
  MDA("ME"), // Moldova, Republic of
  MDG("MF"), // Madagascar
  MDV("MG"), // Maldives
  MEX("MH"), // Mexico
  MHL("MK"), // Marshall Islands
  MKD("ML"), // North Macedonia
  MLI("MM"), // Mali
  MLT("MN"), // Malta
  MMR("MO"), // Myanmar
  MNE("MP"), // Montenegro
  MNG("MQ"), // Mongolia
  MNP("MR"), // Northern Mariana Islands
  MOZ("MS"), // Mozambique
  MRT("MT"), // Mauritania
  MSR("MU"), // Montserrat
  MTQ("MV"), // Martinique
  MUS("MW"), // Mauritius
  MWI("MX"), // Malawi
  MYS("MY"), // Malaysia
  MYT("MZ"), // Mayotte
  NAM("NA"), // Namibia
  NCL("NC"), // New Caledonia
  NER("NE"), // Niger
  NFK("NF"), // Norfolk Island
  NGA("NG"), // Nigeria
  NIC("NI"), // Nicaragua
  NIU("NL"), // Niue
  NLD("NO"), // Netherlands
  NOR("NP"), // Norway
  NPL("NR"), // Nepal
  NRU("NU"), // Nauru
  NZL("NZ"), // New Zealand
  OMN("OM"), // Oman
  PAK("PA"), // Pakistan
  PAN("PE"), // Panama
  PCN("PF"), // Pitcairn
  PER("PG"), // Peru
  PHL("PH"), // Philippines
  PLW("PK"), // Palau
  PNG("PL"), // Papua New Guinea
  POL("PM"), // Poland
  PRI("PN"), // Puerto Rico
  PRK("PR"), // Korea (Democratic People's Republic of)
  PRT("PS"), // Portugal
  PRY("PT"), // Paraguay
  PSE("PW"), // Palestine, State of
  PYF("PY"), // French Polynesia
  QAT("QA"), // Qatar
  REU("RE"), // Réunion
  ROU("RO"), // Romania
  RUS("RS"), // Russian Federation
  RWA("RU"), // Rwanda
  SAU("RW"), // Saudi Arabia
  SDN("SA"), // Sudan
  SEN("SB"), // Senegal
  SGP("SC"), // Singapore
  SGS("SD"), // South Georgia and the South Sandwich Islands
  SHN("SE"), // Saint Helena, Ascension and Tristan da Cunha
  SJM("SG"), // Svalbard and Jan Mayen
  SLB("SH"), // Solomon Islands
  SLE("SI"), // Sierra Leone
  SLV("SJ"), // El Salvador
  SMR("SK"), // San Marino
  SOM("SL"), // Somalia
  SPM("SM"), // Saint Pierre and Miquelon
  SRB("SN"), // Serbia
  SSD("SO"), // South Sudan
  STP("SR"), // Sao Tome and Principe
  SUR("SS"), // Suriname
  SVK("ST"), // Slovakia
  SVN("SV"), // Slovenia
  SWE("SX"), // Sweden
  SWZ("SY"), // Eswatini
  SXM("SZ"), // Sint Maarten (Dutch part)
  SYC("TC"), // Seychelles
  SYR("TD"), // Syrian Arab Republic
  TCA("TF"), // Turks and Caicos Islands
  TCD("TG"), // Chad
  TGO("TH"), // Togo
  THA("TJ"), // Thailand
  TJK("TK"), // Tajikistan
  TKL("TL"), // Tokelau
  TKM("TM"), // Turkmenistan
  TLS("TN"), // Timor-Leste
  TON("TO"), // Tonga
  TTO("TR"), // Trinidad and Tobago
  TUN("TT"), // Tunisia
  TUR("TV"), // Turkey
  TUV("TW"), // Tuvalu
  TWN("TZ"), // Taiwan, Province of China
  TZA("UA"), // Tanzania, United Republic of
  UGA("UG"), // Uganda
  UKR("UM"), // Ukraine
  UMI("US"), // United States Minor Outlying Islands
  URY("UY"), // Uruguay
  USA("UZ"), // United States of America
  UZB("VA"), // Uzbekistan
  VAT("VC"), // Holy See
  VCT("VE"), // Saint Vincent and the Grenadines
  VEN("VG"), // Venezuela (Bolivarian Republic of)
  VGB("VI"), // Virgin Islands (British)
  VIR("VN"), // Virgin Islands (U.S.)
  VNM("VU"), // Viet Nam
  VUT("WF"), // Vanuatu
  WLF("WS"), // Wallis and Futuna
  WSM("YE"), // Samoa
  YEM("YT"), // Yemen
  ZAF("ZA"), // South Africa
  ZMB("ZM"), // Zambia
  ZWE("ZW"), // Zimbabwe
  ;

  private String twoCharacterCode;

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
