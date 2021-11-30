package com.tranwall.capital.data.model.enums;

import com.tranwall.capital.common.error.InvalidRequestException;
import java.util.Arrays;
import java.util.Optional;

public enum NetworkMessageTransactionType {
  SYS_OPTS_TO_CS("70", "System Opts Out of IVRU to Go to Live CSR"),
  CSR_CALL("71", "Call Customer Service (Per Unit)"),
  CSR_AUTO_CALL("72", "Automatically Call Customer Service (Per Unit)"),
  INST_CARD_ISSUE("IC", "Instant Card Issue"),
  PURCHASE_TRANS("00", "POS Purchase"),
  WITHDRAW_CASH("01", "Cash Withdrawal"),
  ADJUSTMENT("02", "Unsupported Debit Adjustment"),
  CHECK_GUARANTEE("03", "Check Guarantee"),
  CHECK_VERIFICATION("04", "Check Verification"),
  EURO_CHECK2("05", "Eurocheck (Unsuported)"),
  TRAVELLER_CHECKS("06", "Process Traveller Check"),
  LETTER_OF_CREDIT("07", "Letter of Credit"),
  GIRO2("08", "Giro (Unsupported)"),
  CASH_DISBURSEMENT("09", "Goods and Services With Cash Disbursement"),
  NON_CASH_FUND("10", "Financial Non-Cash Funding"),
  BUY_QUASI_CASH("11", "Buy Quasi-Cash and Scrip"),
  DEBIT_FUNDS("12", "Administrative Funds Debit"),
  FUND_WITHDRAW_EP("13", "Funds Withdrawal for Elec. Purchase"),
  CASH_ADVANCE("14", "Adjustment - Cash Advance"),
  EXPRESS_DELIVERY("15", "Card Shipment Express Delivery"),
  OVERNIGHT_DELIVERY("16", "Card Shipment Priority Over Night"),
  CASH_CHECK("17", "Cash Check"),
  DEFERRED_GOODS("18", "Deferred Goods"),
  DEF_GOODS_DISBRUSMENT("19", "Goods and Services With Cash Disbursement"),
  RETURN_TRANS("20", "Purchase Returns"),
  DEPOSIT_PAYMENTS("21", "Load Money Onto Card"),
  DEDUCT_MONEY("22", "Credit Adjustment"),
  CHK_DEPOSIT_GRNTE("23", "Credit Adjustment (Debit MasterCard Only)"),
  CHECK_DEPOSIT("24", "Check Deposit"),
  CREDIT_FUNDS("27", "Administrative Funds Credit"),
  PAYMENT_RETURN("28", "Payment Return"),
  COMMERCIAL_DPST("29", "Commercial Deposit (Advantage-Assinged Private  Value)"),
  SERVICE_BALANCE_INQUIRY("30", "Value Added Service Balance Inquiry"),
  BALANCE_INQUIRY("31", "Balance Inquiry"),
  AC_VERIFICATION("33", "Account Verification"),
  AUTHORIZE_POS("36", "Authorize POS for Network"),
  LOGIN("37", "Login"),
  UPGRADE_TO_MC("38", "Upgrade to Master Card"),
  GENERIC_BAL_INQ("39", "Generic Balance Inquiry"),
  CARD_TO_CARD("40", "Card to Card Transfer (Share Funds)"),
  BANK_AC_REG("43", "Bank Account Registration"),
  MONEY_TRANSFER("44", "Money Transfer"),
  CARD2CARD_SELF("45", "Card to Card Transfer (Self Transfer)"),
  BANK_TO_CARD("46", "Bank to Card Transfer"),
  CARD_TO_BANK("47", "Card to Bank Transfer"),
  CARD_LOAD_TRANS("48", "Batch Load Card to Card"),
  BANK2CARD_TRANS("49", "Batch Load Bank To Card"),
  PAY_OTHER_PARTY("50", "Payment to other Party"),
  INACTIVE_ACC("51", "Inactive Account"),
  SWITCH_TO_CSR1("52", "User Opts Out of IVRU to Go to Live CSR"),
  GET_TRANS_HIST("53", "Get Transaction History"),
  PAYMENT_P2P("54", "Payment Debit (Peer to Peer)"),
  PAYMENT_3RD_PAR("55", "Payment From Third Party"),
  PAYMENT_CR_P2P("56", "Payment Credit (Peer to Peer)"),
  REISSUE_ON_EXPIRY("57", "Reissue Expiring Cards"),
  PAYMENT_CREDIT("58", "Payment From Account to Credit/Loan"),
  PAYMENT_ENCLOSE("59", "Payment Enclosed"),
  PURCHASE_ORDERS("80", "Mail / Phone order"),
  VERIFY_CARD("81", "Card Verification"),
  CHARGE_TRANSACTION("82", "Charge Transaction"),
  INFORMATION_INQUIRY("91", "Information Inquiry"),
  NOTIFICATION_TO_BANK("92", "Notification to Bank"),
  ID_VERIFICATION("93", "ID Verification"),
  CHILD_CARE_BENEFIT("94", "Child Care Benefit"),
  CASH_BENEFIT("96", "Cash Benefit"),
  FOOD_STAMP_BENEFIT("98", "Food Stamp Benefit"),
  VERIFY_BANK_ACCOUNT("A0", "Verify Bank Account for Transfer"),
  ACCESS_CODE("A1", "Get Access Code"),
  SET_ACCESS_CODE("A2", "Set Access Code"),
  RESET_ACCESS_CODE("A3", "Reset Access Code"),
  ACCOUNTS_LIST("A4", "Get Cardholder Accounts"),
  WITHDRAW_ACCOUNT("A5", "Create Bank Account for Withdrawal"),
  LOAD_ACCOUNT("A6", "Create Bank Account for Load"),
  WITHDRAW_LOAD_ACCOUNT("A7", "Create Bank Account for Load & Withdrawal"),
  VALIDATE_ACESS_CODE("A8", "Validate Access Code"),
  BANK_ACCOUNT_INFO("A9", "A9"),
  BANK_ACCOUNT_STATUS("AA", "Get Bank Account Verification Status"),
  CHANGE_ACCOUNT_NICK("AB", "Change Bank Account Nick Name"),
  CHANGE_BANK_INFO("AC", "Change Info For Unverified Bank Account"),
  BANK_ACCOUNT_TRANSFER_INFO("AD", "Get Bank Account Transfer Info"),
  BANK_STATEMENT("AH", "Bank Transfer Statement"),
  ACTIVATE_LOAD("AL", "Activate and Load Card"),
  MINI_BANK_STATEMENT("AM", "Bank Transfer Mini Statement"),
  CREDIT_TRANSFER_BATCH("BC", "Batch Credit Funds Transfer"),
  DEBIT_TRANSFER_BATCH("BD", "Batch Debit Funds Transfer"),
  CLOSE_ACCOUNT("C1", "Close Account"),
  DEACTIVATE_CARD("D1", "Deactivate Card"),
  ACTIVATE_CARD("D2", "Activate Card"),
  REISSUE_LOST_STOLEN_CARD("D3", "Reisse Lost / Stolen Card"),
  DIRECT_DEPOSIT("DD", "Direct Deposit"),
  TRANSFER_FUNDS("DT", "Direct Linked Cards Transfer"),
  HOST_CHARGE("F1", "Acquirer Charges"),
  REVERSE_HOST_FEE("F2", "Acquirer Charges Reverse"),
  INTERNATIONAL_TRANSFER("IT", "International Funds Transfer (Debit)"),
  LINKED_CARDS("LC", "Get Linked Cards"),
  MINI_CARD_STATEMENT("M1", "Generate Card Mini Statement"),
  ACCOUNT_MAINTENANCE("MA", "Account Maintenance"),
  NEW_CARD_PURCHASE("NC", "New Card Purchase"),
  UPDATE_PROFILE("P1", "Update Profile (OFAC/AVS)"),
  REISSUE_CARD("RC", "Reissue Card (Same Card Number)"),
  CARD_STATUS("S1", "Get Card Status"),
  SET_CARD_STATUS("S2", "Set Card Status"),
  SET_LINKED_CARDS("SL", "Link Cards"),
  TRANS_DETAIL("T1", "Get Transaction Info"),
  PRE_AUTH_TRANS("T2", "Pre Authorization"),
  CSR_POSTS_TRANS("T3", "Force Post Transaction"),
  REVERSAL("T4", "Reverse Transaction"),
  UPGRADE_CARD("UC", "Upgrade Card"),
  UNLINKED_CARDS("UL", "Unlink Cards"),
  VAS_BAL_INQUIRY("VB", "Value Added Service Balance Inquiry"),
  LOAD_VAS_CREDIT("VC", "Value Added Services Credit"),
  WITHDRAW_FROM_VAS("VD", "Value Added Services Debit"),
  TRANSFER_FROM_VAS("VF", "Funds Transfer from Value Added Service to Card  Account"),
  VAS_STATEMENT("VH", "VAS Transaction Statement"),
  MINI_VAS_STATEMENT("VM", "VAS Transaction Mini Statement"),
  VAS_PRE_AUTH("VP", "VAS Pre Authorization"),
  TRANSFER_TO_VAS("VT", "Funds Transfer from Card to Value Added Service  Account"),
  GET_VAS_ACCOUNT_NUMBER("GV", "Get Value Added Service Account Number"),
  EURO_CHECK1("5", "Eurocheck (Unsuported)"),
  GIRO1("8", "Giro (Unsupported)"),
  BATCH_DB_C2C("CD", "Batch Debit Funds Transfer"),
  BATCH_CR_C2C("CC", "Batch Credit Funds Transfer"),
  BATCH_DB_ACH("HD", "Batch Debit ACH"),
  BATCH_CR_ACH("HC", "Batch Credit ACH"),
  CHARGEBACK_CR("CB", "Chargeback Credit"),
  CB_ACPT("CA", "Chargeback Acceptance"),
  CB_REJCT("CR", "Chargeback Rejection"),
  GET_CARDHOLDER_BILL_PAYEES_IVR("GP", "Get Cardholder Bill Payment Payees (IVR)"),
  GETBPSTATS("BS", "Get Bill Payment Status"),
  BP_REVERSE_TRAN("BR", "Bill Payment Reverse Transaction"),
  BP_HISTORY("BT", "Bill Payment Statement"),
  BILL_PAYMENT("BP", "Bill Payment"),
  GET_CARDHOLDER_BILL_PAYEES("BY", "Get Cardholder Bill Payment Payees"),
  BP_CANCEL_TRAN("BN", "Bill Payment Cancellation Transaction"),
  BP_ALERTS_DEF("BA", "Bill Payment Alerts Definition"),
  CRDINACTIVITY("IF", "Card Inactivity"),
  PREAUTHCANCEL("PC", "Preauth Cancelation"),
  ACCOUNTSETUP("AS", "Account Setup"),
  VAS_PREAUTH_EXP("VE", "VAS Pre Auth Expiry"),
  REVWRITE_OFF("FW", "Reversal Write-Off"),
  ADMIN_WRITE_OFF("AW", "Admin Credit Write-Off"),
  VALIDATE_SECRET("VS", "Validate Secret Code"),
  CR_REG("RG", "Cardholder Registration"),
  GET_CARD_CVV2("GS", "Get Card CVV2"),
  ADDSTKHLDRRECRD("SA", "Add Stakeholder Record"),
  UPDSTKHLDRRECRD("US", "Update Stakeholder Record"),
  DELSTKHLDRRECRD("DS", "Delete Stakeholder Record"),
  ASSGNREASSGNRNG("SR", "Assign/Re-Assign Card Range"),
  UNASSIGNRANGE("UR", "UnAssign Card Range"),
  FAXDIRDEPFORM("FD", "FAX Direct Deposit Form"),
  GETDIRDEPINFO("GD", "Get Direct Deposit Information"),
  NEW_CARD_SO("SO", "New Card Sales Order"),
  CASHREBATE("RB", "Cash Rebate"),
  A_C_STATEMENT("ST", "Statement of account"),
  UPDATE_FREE_CAL("TU", "Update Free Call Information"),
  VAS_REVERSAL("VR", "VAS Reversal"),
  SVC_PAYMENT("SP", "SVC Payment"),
  ADVANCEPAYMENT("AP", "Advance Payment"),
  GETPO("N1", "Get Purchase Orders - TVP"),
  GETPODETAILS("N2", "Get Purchase Order Details - TVP"),
  FULLPAYPOCRD("N3", "Full Payment of a Purchase Order by Credit Card"),
  FULLPAYPOBNK("N5", "Full Payment of a Purchase Order by Bank Account"),
  ISSUE_CHECK("IK", "Issue check"),
  ESCHEAT_FUND("EF", "Escheat Fund"),
  DIRECT_DEPOSITD("DB", "Direct Deposit Debit"),
  SUPPLEMENT_CARD("SC", "Supplementary Card Purchase Order"),
  GET_LDCAL_PHONE("L1", "Get Phone Number List for Long Distance Calls"),
  ADD_LDCAL_PHONE("L2", "Add Phone Numbers for Long Distance Calls"),
  DEL_LDCAL_PHONE("L3", "Delete Phone Numbers for Long Distance Calls"),
  LOAD_COUPON("P0", "Load Coupon"),
  VERIFYPHONELDC("L4", "Verify Phone No for Long Distance Calls"),
  OVERNIGHTTRASFR("OT", "Overnight Money Transfer"),
  VRFY_RELOAD("U1", "Verify Further Reload Eligibility"),
  ICOUPONCR("PW", "i-Coupon credit"),
  RESETPIN("P4", "Reset Card PIN"),
  VALIDATEPIN("P5", "Validate Card PIN"),
  PINDELIVERY("P6", "PIN Delivery Request"),
  PROVISIONALCRDT("PV", "Provisional Credit"),
  CASHREWARDS("CW", "Cash Rewards"),
  MRAY("MS", "MRay"),
  C2CTRASFRGIFT("GT", "Card to Card Transfer (Gift Money)"),
  CR2DTCARDXFR("GM", "Credit Card to Debit Card Transfer (Gift Money)"),
  LOST_STOLENSTS("LS", "Set Card Status to Lost / Stolen"),
  GETCARDHOLDER("CP", "Get Cardholder Profile"),
  CRDHLDRINQUIRY("CI", "Cardholder Inquiry Request"),
  LOGCARDHOLDERRQ("C5", "Log Cardholder Request"),
  SAVEGIFTOGRAM("G2", "Save Giftogram Recording"),
  GETGIFTOGRAM("G3", "Retrieve Giftogram"),
  FIRSTLOAD("FL", "First Load on Card"),
  VERIFYPURCHASE("S5", "Get Card Status for Card Purchase"),
  CARDPURCHASE("S6", "Card Purchase"),
  ACPTDECLCOUPON("C2", "Accept Decline i-Coupon"),
  LOGTRANSACTION("LT", "Log Transaction"),
  PAYMENTREISSUE("PR", "Payment Reissue"),
  SHAREFUNDSAGREG("FA", "Share Funds Aggregate"),
  SHAREFUNDSSPLIT("FS", "Share Funds Split"),
  GETCHPROFILEADV("C8", "Get Cardholder Profile Advance"),
  CHANGECARDPROG("CG", "Change Card Program"),
  LOADCENTERLOC("LL", "Load Center Locator"),
  ORGCREDIT("26", "Original Credit"),
  EXCHANGEPPK("EK", "Exchange PIN Protection Key"),
  LIST_BANKACC("B1", "List Bank Accounts"),
  REMOVEBANKACC("B2", "Remove Bank Account"),
  LISTB2CTRASFR("B4", "List Bank to Card Transfers"),
  CANCELB2CTRASFR("B6", "Cancel Bank to Card Transfer"),
  CREATEALERT("R1", "Create Alert"),
  ALERTLIST("R2", "Alert List"),
  REMOVEALERT("R5", "Remove Alert"),
  PROMOENRLMNT("C4", "Promo Enrollment"),
  C2CTRASFR("42", "Card to Card Transfer"),
  CREDITTRANSSP("SE", "Credit Funds Transfer Special"),
  DEBITTRANSSP("SD", "Debit Funds Transfer Special"),
  ENVCASHDEPOSIT("25", "Envelopeless Cash Deposit"),
  PROMOOPTOUT("C6", "Promo Opt Out"),
  MRAYUNSUB("MU", "MRay UnSubscription"),
  LISTC2BTRASFR("B7", "List Card to Bank Transfers"),
  UPDATEC2BTRNSFR("B8", "Update Card to Bank Transfer"),
  CANCELC2BTRASFR("B9", "Cancel Card to Bank Transfer"),
  PROCESSINGTAX("TX", "Processing Tax"),
  TRASFERHISTORY("TH", "Get Transfer History"),
  MONEYTRASFRDB("MT", "Money Transfer Debit"),
  GETTRANSHISTORY("HT", "Get Transaction History"),
  RESETEXPIRY("RX", "Reset Card Expiry"),
  BALINQADV("BI", "Balance Inquiry Advance"),
  SYSOPTSTOCS("OU", "System Opts Out of IVRU to Go to Live CS"),
  GETMERCHANT("MG", "Get Merchant Stores"),
  BENEFITSCREDIT("BZ", "Benefits Credit"),
  SWITCH_TO_CSR2("AU", "User Opts Out of IVRU to Go to Live CSR"),
  CRCD2SVCLDFNDS("CL", "Credit Card to SVC Load Funds"),
  RECLVLBATCHFEE("RF", "Record level batch fee"),
  EMBEDDEDTRANS("EM", "Embedded Service"),
  MULTIPURSEBI("PI", "Multi Purse Balance Inquiry"),
  MULTIPURSEHIST("PH", "Transaction History Multi Purse"),
  DDHISTORY("DH", "Direct deposit transaction history"),
  RETAILAGENTLOAD("RL", "Retail Agent Load"),
  BLACKHAWKPOSLOD("BH", "Black Hawk POS Load"),
  SPECIALCREDIT1("Q1", "Special Credit 1"),
  SPECIALCREDIT2("Q2", "Special Credit 2"),
  PAYEXCHPOSLOAD("PX", "Pay Exchange POS Load"),
  RETAILAGNTDEBIT("RD", "Retail Agent Debit"),
  SPECIALDEBIT1("W1", "Special Debit 1"),
  SPECIALDEBIT2("W2", "Special Debit 2"),
  TOPUPW_BONUS("K1", "Top up card with bonus"),
  TOPUPW_OBONUS("K2", "Top up card without bonus"),
  ADJCRDTFORBONUS("K3", "Adjust credit for bonus"),
  ADJDEBTFORBONUS("K4", "Adjust debit for bonus"),
  PROFILEAUTH("P7", "Cardholder Profile Authentication"),
  I_BANKTOCARD("AE", "Instant Bank to Card Transfer"),
  I_CARDTOBANK("AF", "Instant Card to Bank Transfer"),
  VOUCHERLOAD("VL", "Load voucher on card"),
  INTERACLOADFUND("LI", "Load funds on card from Interac Payment Gateway"),
  GETSIGNONTKN("SG", "Get SignOn Token"),
  VALIDSIGNONTKN("SV", "Validate SignOn Token"),
  EXPIRESIGNONTKN("SX", "Expire SignOn Token"),
  EDITPAYMENT("EP", "Edit Payment"),
  ADDPAYEE("RP", "Add Payee"),
  EDITPAYEE("RE", "Edit Payee"),
  DELETEPAYEE("KP", "Delete Payee"),
  SEARCHPAYEE("HP", "Search Payee"),
  GETEXTSIGNONTKN("ES", "Get External SignOn Token"),
  PURCHASEICOUPON("IP", "Purchase ICoupon"),
  PURCHASEINFO("N7", "Get Purchase Info"),
  GETPURSES("GA", "Get Purses"),
  CARDVERIFADV("CV", "Card Verification Advance"),
  VERIFY_PRODUCT_PURCHASE("S3", "Verify Product Purchase"),
  PRODUCT_PURCHASE("PU", "Product Purchase"),
  USERREG("RU", "User Registration"),
  EXTERNALUNSUBS("SI", "External Unsubscribe"),
  EXTRNLACTVATION("EA", "External Activation"),
  GIVELIST("GL", "Give List"),
  CARDPIN("P2", "Get Card PIN"),
  CHPROFLOOKUP("PP", "CardHolder Profile Lookup"),
  VERIFYPAYMTREFR("R4", "Verify Payment Reference Number"),
  FTREGISTRATION("I1", "Registration for Funds Transfer"),
  ADDBENEFICIARY("I2", "Add Beneficiary Bank Account"),
  FUNDSTRASFR("I3", "Funds Transfer"),
  TRASFRREFUND("I4", "Funds Transfer Refund"),
  TRASFRREJECT("I5", "Funds Transfer Reject"),
  OVERDRAFT("OD", "Overdraft Transaction Service"),
  CHARITYDONATION("CH", "Charity Donation"),
  PURCHASES("41", "Purchase"),
  B2CBATCHCR("BB", "Batch Load Bank To Card"),
  C2CBATCHCR("BL", "Batch Load Card to Card"),
  INCON_B_WPRTSCR("K5", "Inconsistency between two parties Credit"),
  INCON_B_WPRTSDB("K6", "Inconsistency between two parties Debit"),
  VERIFYACT("AV", "Verify Activation"),
  PINCHANGE("90", "PIN Change"),
  SETPIN("P3", "Set Card PIN"),
  LOGB2CTRASFR("B3", "Log Bank to Card Transfer"),
  UPDATEALERT("R3", "Update Alert"),
  MERCHANTLOC("ML", "Merchant Locations"),
  GETCOUPONLIST("C3", "Get i-Coupons List"),
  LOADMERCHANTS("LM", "Load Center Merchants"),
  UPDATEB2CTRASFR("B5", "Update Bank to Card Transfer"),
  APPLYCARDRESTRICTION("AR", "Apply Card Restriction"),
  REMOVECARDRESTRICTION("RR", "Remove Card Restriction"),
  WITHDRAWCASH("OS", "Cash Withdrawal"),
  REMOVE_PURSE("DP", "Remove Purse"),
  ADD_PURSE("NP", "Add Purse"),
  DRADMINC2C("DM", "Administrative Funds C2C Debit"),
  CRADMINC2C("CM", "Administrative Funds C2C Credit"),
  GETPININFO("P8", "Get PIN Info"),
  GETPININFOADVANCE("P9", "Get PIN Info Advance"),
  GETSHIINFO("I7", "Get Shipment Information"),
  AUTOADJUSTMENTADVICETO_ZEROIZE("AJ", "Auto adjustment advice to zeroize -ive balance account"),
  CRDPAYINFO("I6", "Credit Payment Info"),
  GETCHSTMT("I8", "Get Cardholder Statement"),
  GETCARDACCNTS("I9", "Get Card Accounts"),
  TRANSHISTMULTIACCOUNTS("J1", "Transaction History Multi Accounts"),
  MINISTMTADVNC("J3", "Mini Statement Advance"),
  LATEPAYMNT("LF", "Late Payment"),
  FINCHARGES("FC", "Financial Charges"),
  BALTSFRINIT("IB", "Balance Transfer Initiation"),
  BATCH_LOAD_CP_C2C("BF", "Batch Load CP C2C"),
  POSTALEVNTLOGNG("Q8", "Postal Event Logging"),
  AUTOBANK2CARD("UT", "AutoBank to Card Transfer"),
  AUTOCARDTOBANK("WA", "AutoCard to Bank Transfer"),
  STAMPDUTYFEE("SF", "Stamp Duty Fee"),
  DORMANCYFEE("DR", "Dormancy Fee"),
  USEQUOTE("UQ", "Use Quote Info"),
  GETEXCHRATES("GE", "Get Exchange Rates"),
  GETQUOTE("GQ", "Get Quote Info"),
  IFTCANCELLATION("J4", "IFT Cancellation"),
  SETEXCHRATES("UE", "Set Exchange Rates"),
  CARD_TO_CARD_SU("WE", "Card to Card Transfer For SupCH"),
  GETPROGINFO("PB", "Get Program Info"),
  PAYMENTTRANSHIS("MH", "Get Payment Transfer History"),
  SENDPAYMENT("SM", "Send Payment"),
  CANCELPAYMNT("J5", "Cancel Send Payment Request"),
  MONTHINSPRIM("MI", "Monthly Insurance Premium"),
  DECLINEQUOTE("DQ", "Decline Quote"),
  CHECK_CREDIT("CK", "LOCKBOX CHECK CREDIT"),
  CHECK_BOUNCE_FEE("CF", "LOCKBOX BOUNCED FEE"),
  ATM2SVCLDFNDS("K9", "Bank (ATM) to SVC Load Funds"),
  STOR2SVCLDFNDS("K8", "Convenience Store to SVC Load Funds"),
  INBK2SVCLDFNDS("K7", "Internet Banking to SVC Load Funds"),
  RESENDECARDINFO("RS", "Resend Virtual Card Info"),
  VALIDATEPREACTIVATION("J2", "Validate Pre Activation"),
  LINKEXTRNLCARD("EL", "Link External Card"),
  UPDATEEXTRNLCARD("J6", "Update External Card Profile"),
  REMOVEEXTCARD("J7", "Remove External Card"),
  CASHLOAD("CO", "Cash Load"),
  CHECKLOAD("CQ", "Check Load"),
  SHIP_TO_CH_ON("SN", "Ship To Cardholder Overnight"),
  SHIP_TO_PR_ON("PN", "Ship To Purchaser Overnight"),
  SHIP_TO_CH_USPS("SU", "Ship To Cardholder USPS"),
  SHIP_TO_PR_USPS("PS", "Ship To Purchaser USPS"),
  CC2SVCSALESORDR("L5", "New Card Sales Order, Credit card"),
  ATM2SVCSALESORDR("L6", "New Card Sales Order, Bank (ATM)"),
  STOR2SVCSALESORDR("L7", "New Card Sales Order, Convenience Store"),
  INBK2SVCSALESORDR("L8", "New Card Sales Order, Internet Banking"),
  TKNSTSUPDT("TK", "Token Status Update"),
  TKNEXPRYUPDT("TE", "Token Expiry Update"),
  TKNCARDMAPUPDT("TM", "Token Card Mapping Update"),
  UNCLAIMED_MONEY("UM", "UnClaimed money"),
  SUSPFRAUDTRANS("FT", "Suspected Fraudulent Transaction"),
  STMTDATECHG("SZ", "Statement Date Change"),
  ADDCHMISCINFO("M5", "Add CH Misc Info"),
  UPDCHMISCINFO("M6", "Update CH Misc Info"),
  DELCHMISCINFO("M7", "Delete CH Misc Info"),
  GETCHMISCINFO("M8", "Get CH Misc Info"),
  EXTLINKCRDS("M9", "Get External Linked Cards"),
  CHECKDEPOSITFEE("O1", "Check Deposit Fee"),
  CREDITFUNDSFEE("O2", "Administrative Funds Credit Fee"),
  PYMTRETACTLDFEE("O3", "Payment Return/Prepaid Activation and Load/Prepaid  Load Fee"),
  ACCVERFCATONFEE("O4", "Account Verification Fee"),
  XTRAHOLDMULTICU("XM", "XtraHold On MultiCurrency Service"),
  GETCARDORDDETAIL("GO", "Get Card Order Details"),
  CHARGEBACKDRADJ("DC", "Chargeback Debit Adjustment"),
  LNKCRDUSMEMID("IM", "Link Card Using Member Id"),
  ULNKCRDUSMEMID("KM", "UnLink Card Using Member Id"),
  LOGOUT("UO", "Logout"),
  ALCASHOUTPAYORDER("4O", "All Cash Out Pay Order"),
  ALCASHOUTWUMONYTRSFR("5O", "All Cash Out WU Money Transfer"),
  EMGYFUNDPAYORDER("6O", "Emergency Funds Pay Order"),
  EMGYFUNDWUMONYTRSFR("7O", "Emergency Funds WU Money Transfer"),
  ALERTFEE("R6", "Alert Fee"),
  RECLAIMFUNDSCR("2O", "Reclaim Funds Credit"),
  RECLAIMFUNDSDR("3O", "Reclaim Funds Debit"),
  GETCHRSTRCTNS("R9", "Get Cardholder Restrictions"),
  GET_MERCHANT_LI("M2", "Get Merchant List"),
  GET_MERCHANT_DE("M3", "Get Merchant Details"),
  ;

  public String getI2cTransactionType() {
    return i2cTransactionType;
  }

  private final String i2cTransactionType;
  private final String description;

  NetworkMessageTransactionType(String i2cTransactionType, String description) {
    this.i2cTransactionType = i2cTransactionType;
    this.description = description;
  }

  // TODO(kuchlein): make more efficient
  public static NetworkMessageTransactionType fromI2cDeviceType(String i2cDeviceType) {
    Optional<NetworkMessageTransactionType> deviceTypeOptional =
        Arrays.stream(NetworkMessageTransactionType.values())
            .filter(deviceType -> deviceType.i2cTransactionType == i2cDeviceType)
            .findFirst();
    if (deviceTypeOptional.isEmpty()) {
      throw new InvalidRequestException("invalid i2c device type " + i2cDeviceType);
    }

    return deviceTypeOptional.get();
  }
}