package com.clearspend.capital.data.model.security;

import java.util.Set;

/**
 * String constants representing default roles, both global and for allocation roles.
 *
 * <p>This exists to reduce typos and provide a common place for updating roles if they should
 * change.
 */
public interface DefaultRoles {

  String GLOBAL_CUSTOMER_SERVICE_MANAGER = "customer_service_manager";
  String GLOBAL_CUSTOMER_SERVICE = "customer_service";
  String GLOBAL_VIEWER = "global_viewer";
  String GLOBAL_BOOKKEEPER = "bookkeeper";
  String GLOBAL_RESELLER = "reseller";
  String GLOBAL_PROCESSOR = "processor";

  String ALLOCATION_ADMIN = "Admin";
  String ALLOCATION_MANAGER = "Manager";
  String ALLOCATION_VIEW_ONLY = "View only";
  String ALLOCATION_EMPLOYEE = "Employee";

  Set<String> ALL_ALLOCATION =
      Set.of(ALLOCATION_ADMIN, ALLOCATION_MANAGER, ALLOCATION_VIEW_ONLY, ALLOCATION_EMPLOYEE);
  Set<String> ALL_GLOBAL =
      Set.of(
          GLOBAL_RESELLER,
          GLOBAL_CUSTOMER_SERVICE,
          GLOBAL_BOOKKEEPER,
          GLOBAL_VIEWER,
          GLOBAL_CUSTOMER_SERVICE_MANAGER,
          GLOBAL_PROCESSOR);
}
