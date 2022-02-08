package com.clearspend.capital.data.model.enums;

import java.util.EnumSet;
import java.util.Map;

/** Matching the database enum of the same name */
public enum GlobalUserPermission {
  /** These folks can batch-onboard */
  BATCH_ONBOARD,

  /**
   * Grants permission for crossing business boundaries dealing with allocation
   * globalUserPermissions
   */
  CROSS_BUSINESS_BOUNDARY,

  /** e.g. Auditor for ClearSpend information */
  GLOBAL_READ,

  /**
   * ClearSpend customer service can grant globalUserPermissions except CUSTOMER_SERVICE_MANAGER and
   * AUDITOR, and have permission to read accounts, enter transactions (for dispute resolution etc.)
   */
  CUSTOMER_SERVICE,

  /** Creates ClearSpend customer service accounts */
  CUSTOMER_SERVICE_MANAGER,

  /** Callbacks from the processing network */
  SYSTEM;

  /** Key of a role, value of role(s) allowed to grant that permission. */
  public static final Map<GlobalUserPermission, EnumSet<GlobalUserPermission>> GRANTORS =
      Map.of(
          BATCH_ONBOARD,
          EnumSet.of(CUSTOMER_SERVICE, CUSTOMER_SERVICE_MANAGER),
          CROSS_BUSINESS_BOUNDARY,
          EnumSet.of(CUSTOMER_SERVICE, CUSTOMER_SERVICE_MANAGER),
          GLOBAL_READ,
          EnumSet.of(CUSTOMER_SERVICE_MANAGER),
          CUSTOMER_SERVICE,
          EnumSet.of(CUSTOMER_SERVICE_MANAGER));

  public static final EnumSet<GlobalUserPermission> ALL_CUSTOMER_SERVICE =
      EnumSet.of(
          GlobalUserPermission.CUSTOMER_SERVICE, GlobalUserPermission.CUSTOMER_SERVICE_MANAGER);

  public static final EnumSet<GlobalUserPermission> READ_AND_CUSTOMER_SERVICE =
      EnumSet.of(
          GlobalUserPermission.CUSTOMER_SERVICE,
          GlobalUserPermission.CUSTOMER_SERVICE_MANAGER,
          GlobalUserPermission.GLOBAL_READ);
}
