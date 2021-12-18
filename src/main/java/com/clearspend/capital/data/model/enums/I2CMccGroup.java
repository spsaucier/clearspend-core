package com.clearspend.capital.data.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I2CMccGroup {
  UT_MCG_CONFG("Utilities"),
  RS_MCG_CONFG("Retail Stores"),
  AV_MCG_CONFG("Automobiles and Vehicles"),
  MS_MCG_CONFG("Miscellaneous Stores"),
  SP_MCG_CONFG("Service Providers"),
  PS_MCG_CONFG("Personal Service Providers"),
  BS_MCG_CONFG("Business Services"),
  RR_MCG_CONFG("Repair Services"),
  AE_MCG_CONFG("Amusement and Entertainment"),
  SM_MCG_CONFG("Professional Services and Membership Organizations"),
  GS_MCG_CONFG("Government Services"),
  CR_MCG_CONFG("Clothing Stores"),
  CS_MCG_CONFG("Contracted Services"),
  AL_MCG_CONFG("Airlines"),
  AR_MCG_CONFG("Auto Rentals"),
  HM_MCG_CONFG("Hotels and Motels"),
  TT_MCG_CONFG("Transportation"),
  TE_MCG_CONFG("Travel and Entertainment"),
  MSL_MCG_CONFG("Miscellaneous"),
  WS_MCG_CONFG("Wholesale Distributors and Manufacturers"),
  OP_MCG_CONFG("Mail / Phone Order Providers"),
  DG_MCG_CONFG("Digital Goods / Media"),
  RES_MCG_CONFG("Restaurants"),
  GAS_MCG_CONFG("Gas Stations"),
  EDU_MCG_CONFG("Education");

  private final String i2cName;
}
