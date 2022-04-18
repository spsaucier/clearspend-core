-- ####################################################################################################################################################################
-- FUNCTIONS FOR RETRIEVING PERMISSIONS FOR A USER
-- Given the complex permissions matrix that applies to any given user, the functions contained within this file attempt to
-- handle retrieving said permissions in a manner as simple as such a complex workflow can be made.
-- DOCUMENTATION
-- https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2107768940/Permission+Retrieval+in+Capital-Core+SQL+Functions

-- DEFINITIONS
-- Grantee = This represents the user that we are trying to get permissions for. It will also contain information about the business that user belongs to.
--          The reason for this is that the business the user is retrieving permissions for may not be the same business the user belongs to, however the
--          status of the business the user belongs to is still important for evaluating level of access.
-- Grantor = This represents the business that the user is trying to access, ie the one that we are returning permissions for.

-- SECTIONS
-- 1) Types
-- 2) Global Permissions Functions (these you call from other SQL and/or application)
-- 3) Access Checking Functions
-- 4) Conditional Logic Functions
-- 5) All Permissions Functions (these you call from other SQL and/or application)
-- ####################################################################################################################################################################

-- ####################################################################################################################################################################
-- SECTION 1 = TYPES
-- The Grantee and Grantor are looked up immediately when beginning permissions evaluation in the public functions, and are then passed around to all the
-- helper functions during evaluation. To help with readability and type-safety, these types have been created to wrap around the Grantee/Grantor values.
-- ####################################################################################################################################################################
DROP TYPE IF EXISTS grantee CASCADE;
CREATE TYPE grantee AS (
    user_id UUID, user_type VARCHAR, business_status VARCHAR,
    first_name BYTEA, last_name BYTEA, business_id UUID,
    has_cross_business_permission BOOLEAN
);
ALTER TYPE grantee OWNER TO postgres;
DROP TYPE IF EXISTS grantor CASCADE;
CREATE TYPE grantor AS (
    business_id UUID, business_status VARCHAR
);
ALTER TYPE grantor OWNER TO postgres;

-- ####################################################################################################################################################################
-- SECTION 2 = GLOBAL PERMISSIONS FUNCTIONS
-- Functions to retrieve global permissions. This function is safe to call from the application or from other SQL queries.
-- ####################################################################################################################################################################

-- For a given User and array of global roles, return the global permissions they have access to
-- This does not just return permissions for the roles, it filters them based on user/business status as well
CREATE OR REPLACE FUNCTION get_global_permissions(in_user_id UUID, roles VARCHAR[])
    RETURNS globaluserpermission[]
    LANGUAGE plpgsql
    STABLE
AS
$$
    DECLARE
        business_status_suspended VARCHAR = 'SUSPENDED';
        business_status_closed VARCHAR = 'CLOSED';
        result_permissions globaluserpermission[];
    BEGIN
        SELECT (
            CASE
                WHEN array_agg(c) IS NULL THEN ARRAY[]::globaluserpermission[]
                ELSE array_agg(c)
            END
        )::globaluserpermission[]
        INTO result_permissions
        FROM (
            SELECT DISTINCT unnest(permissions)
            FROM global_roles gr
            JOIN users ON users.id = in_user_id
            JOIN business ON users.business_id = business.id
            WHERE roles @> ARRAY[gr.role_name]
            AND gr.is_application_role = false
            AND business.status NOT IN (business_status_suspended, business_status_closed)
            AND users.archived = FALSE
            UNION ALL
            SELECT DISTINCT unnest(permissions)
            FROM global_roles gr
            WHERE roles @> ARRAY[gr.role_name]
            AND gr.is_application_role = true
        ) AS dt(c);

        RETURN result_permissions;
    END;
$$;
ALTER FUNCTION get_global_permissions(in_user_id UUID, roles VARCHAR[]) OWNER TO postgres;

-- ####################################################################################################################################################################
-- SECTION 3 = ACCESS CHECKING FUNCTIONS
-- There are a variety of checks that are performed in the permissions functions to determine what level of access a user should have.
-- It is far more complicated than just checking their role, there are a wide range of factors that can impact the final permissions result.
-- These are a series of functions that perform the various checks which are then re-used to enforce those checks in other functions.
-- ####################################################################################################################################################################

-- Ensures that the Grantee is allowed to access the Grantor business.
CREATE OR REPLACE FUNCTION has_access_to_business(the_grantee grantee, the_grantor grantor)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
AS
$$
    BEGIN
        RETURN the_grantee.business_id = the_grantor.business_id OR
               -- If they have cross business access, it will then validate later if they have been assigned to an allocation in that business
               the_grantee.has_cross_business_permission = TRUE;
    END;
$$;
ALTER FUNCTION has_access_to_business(the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Checks if either the Grantee or Grantor business is suspended.
CREATE OR REPLACE FUNCTION is_either_suspended(the_grantee grantee, the_grantor grantor)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
AS
$$
    BEGIN
        -- If either are null, that means the WHERE clause on suspended status returned no records, so they are suspended.
        RETURN the_grantee.business_id IS NULL OR the_grantor.business_id IS NULL;
    END;
$$;
ALTER FUNCTION is_either_suspended(the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Checks if either the Grantee or Grantor business is closed
CREATE OR REPLACE FUNCTION is_either_closed(the_grantee grantee, the_grantor grantor)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
AS
$$
    DECLARE
        business_status_closed VARCHAR = 'CLOSED';
    BEGIN
        RETURN the_grantee.business_status = business_status_closed OR the_grantor.business_status = business_status_closed;
    END;
$$;
ALTER FUNCTION is_either_closed(the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Checks if the Grantee is the Business Owner for the Grantor business
CREATE OR REPLACE FUNCTION is_grantee_business_owner_on_grantor(the_grantee grantee, the_grantor grantor)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
AS
$$
    DECLARE
        user_type_business_owner VARCHAR = 'BUSINESS_OWNER';
    BEGIN
        RETURN the_grantee.user_type = user_type_business_owner AND the_grantee.business_id = the_grantor.business_id;
    END;
$$;
ALTER FUNCTION is_grantee_business_owner_on_grantor(the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- ####################################################################################################################################################################
-- SECTION 4 = CONDITIONAL LOGIC FUNCTIONS
-- Roles and permissions are determined by a variety of factors. To help simplify the overall logic for determining them, the conditional
-- logic has been broken up into individual sub-functions. Each function returns a small piece of the overall puzzle based on Grantee/Grantor
-- conditions evaluated by the functions in Section 3.
-- ####################################################################################################################################################################

-- Returns the name of the role on the root allocation based on a variety of Grantee/Grantor driven checks
CREATE OR REPLACE FUNCTION get_root_role_name(definition_permissions allocationpermission[], definition_role_name VARCHAR, the_grantee grantee, the_grantor grantor)
    RETURNS VARCHAR
    LANGUAGE plpgsql
AS
$$
    DECLARE
        result VARCHAR;
        role_name_admin VARCHAR = 'Admin';
        role_name_view_only VARCHAR = 'View only';
        should_have_read_permission BOOLEAN;
        read_permissions allocationpermission[] = ARRAY['READ', 'VIEW_OWN']::allocationpermission[];
    BEGIN
        SELECT definition_permissions @> read_permissions
        INTO should_have_read_permission;

        CASE
            WHEN is_either_suspended(the_grantee, the_grantor) = TRUE OR
                (is_either_closed(the_grantee, the_grantor) AND should_have_read_permission = FALSE) OR
                has_access_to_business(the_grantee, the_grantor) = FALSE THEN
                result = NULL;
            WHEN is_either_closed(the_grantee, the_grantor) = TRUE AND should_have_read_permission = TRUE THEN
                result = role_name_view_only;
            WHEN is_either_closed(the_grantee, the_grantor) = TRUE AND
                is_grantee_business_owner_on_grantor(the_grantee, the_grantor) THEN
                result = role_name_view_only;
            WHEN is_grantee_business_owner_on_grantor(the_grantee, the_grantor) THEN
                result = role_name_admin;
            ELSE
                result = definition_role_name;
        END CASE;

        RETURN result;
    END;
$$;
ALTER FUNCTION get_root_role_name(definition_permissions allocationpermission[], definition_role_name VARCHAR, the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Returns the permissions on the root allocation based on a variety of Grantee/Grantor driven checks.
CREATE OR REPLACE FUNCTION get_root_permissions(definition_permissions allocationpermission[], the_grantee grantee, the_grantor grantor)
    RETURNS allocationpermission[]
    LANGUAGE plpgsql
    STABLE
AS
$$
    DECLARE
        result allocationpermission[];
        role_name_admin VARCHAR = 'Admin';
        should_have_read_permission BOOLEAN;
        read_permissions allocationpermission[] = ARRAY['READ', 'VIEW_OWN']::allocationpermission[];
    BEGIN
        SELECT definition_permissions @> read_permissions
        INTO should_have_read_permission;

        CASE
            WHEN is_either_suspended(the_grantee, the_grantor) OR
                (is_either_closed(the_grantee, the_grantor) AND should_have_read_permission = FALSE) OR
                has_access_to_business(the_grantee, the_grantor) = FALSE THEN
                result = ARRAY[]::allocationpermission[];
            WHEN is_either_closed(the_grantee, the_grantor) AND should_have_read_permission = TRUE THEN
                result = read_permissions;
            WHEN is_either_closed(the_grantee, the_grantor) = TRUE AND
                is_grantee_business_owner_on_grantor(the_grantee, the_grantor) THEN
                result = read_permissions;
            WHEN is_grantee_business_owner_on_grantor(the_grantee, the_grantor) = TRUE THEN
                SELECT allocation_role_permissions.permissions
                INTO result
                FROM allocation_role_permissions
                WHERE allocation_role_permissions.role_name = role_name_admin;
            ELSE
                result = definition_permissions;
        END CASE;

        RETURN result;
    END;
$$;
ALTER FUNCTION get_root_permissions(definition_permissions allocationpermission[], the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Determine whether to use the permissions of the current (child) allocation or inherit them from the parent.
-- IMPORTANT: This is based on permissions array length, ie more permissions equals higher-level role. This is bad
-- for the current state of View Only vs Employee, however it should work for the future state of these roles, so
-- it is being left this way.
CREATE OR REPLACE FUNCTION do_inherit_permissions(child_permissions allocationpermission[], parent_permissions allocationpermission[])
    RETURNS BOOLEAN
    LANGUAGE plpgsql
AS
$$
    BEGIN
        RETURN CASE
            WHEN child_permissions IS NULL THEN TRUE
            WHEN parent_permissions IS NULL THEN FALSE
            WHEN array_length(parent_permissions, 1) > array_length(child_permissions, 1) THEN TRUE
            ELSE FALSE
        END;
    END;
$$;
ALTER FUNCTION do_inherit_permissions(child_permissions allocationpermission[], parent_permissions allocationpermission[]) OWNER TO postgres;

-- Get the role name for the current (child) allocation based on various Grantee/Grantor checks.
CREATE OR REPLACE FUNCTION get_child_role_name(child_permissions allocationpermission[], parent_permissions allocationpermission[], child_role_name VARCHAR, parent_role_name VARCHAR, the_grantee grantee, the_grantor grantor)
    RETURNS VARCHAR
    LANGUAGE plpgsql
AS
$$
    DECLARE
        result VARCHAR;
        role_name_admin VARCHAR = 'Admin';
        role_name_view_only VARCHAR = 'View only';
        should_have_read_permission BOOLEAN;
        read_permissions allocationpermission[] = ARRAY['READ', 'VIEW_OWN']::allocationpermission[];
    BEGIN
        SELECT child_permissions @> read_permissions OR parent_permissions @> read_permissions
        INTO should_have_read_permission;

        CASE
            WHEN is_either_suspended(the_grantee, the_grantor) = TRUE OR
                (is_either_closed(the_grantee, the_grantor) AND should_have_read_permission = FALSE) OR
                has_access_to_business(the_grantee, the_grantor) = FALSE THEN
                result = NULL;
            WHEN is_either_closed(the_grantee, the_grantor) = TRUE AND should_have_read_permission = TRUE THEN
                result = role_name_view_only;
            WHEN is_grantee_business_owner_on_grantor(the_grantee, the_grantor) = TRUE THEN
                result = role_name_admin;
            WHEN do_inherit_permissions(child_permissions, parent_permissions) = TRUE THEN
                result = parent_role_name;
            ELSE
                result = child_role_name;
        END CASE;

        RETURN result;
    END;
$$;
ALTER FUNCTION get_child_role_name(parent_permissions allocationpermission[], parent_permissions allocationpermission[], child_role_name VARCHAR, parent_role_name VARCHAR, the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Get the permissions for the current (child) allocation based on various Grantee/Grantor checks.
CREATE OR REPLACE FUNCTION get_child_permissions(child_permissions allocationpermission[], parent_permissions allocationpermission[], the_grantee grantee, the_grantor grantor)
    RETURNS allocationpermission[]
    LANGUAGE plpgsql
    STABLE
AS
$$
    DECLARE
        result allocationpermission[];
        role_name_admin VARCHAR = 'Admin';
        should_have_read_permission BOOLEAN;
        read_permissions allocationpermission[] = ARRAY['READ', 'VIEW_OWN']::allocationpermission[];
    BEGIN
        SELECT child_permissions @> read_permissions OR parent_permissions @> read_permissions
        INTO should_have_read_permission;

        CASE
            WHEN is_either_suspended(the_grantee, the_grantor) = TRUE OR
                (is_either_closed(the_grantee, the_grantor) AND should_have_read_permission = FALSE) OR
                has_access_to_business(the_grantee, the_grantor) = FALSE THEN
                result = ARRAY[]::allocationpermission[];
            WHEN is_either_closed(the_grantee, the_grantor) = TRUE AND should_have_read_permission = TRUE THEN
                result = read_permissions;
            WHEN is_grantee_business_owner_on_grantor(the_grantee, the_grantor) = TRUE THEN
                SELECT permissions
                INTO result
                FROM allocation_role_permissions
                WHERE allocation_role_permissions.role_name = role_name_admin;
            WHEN do_inherit_permissions(child_permissions, parent_permissions) = TRUE THEN
                result = parent_permissions;
            ELSE
                result = child_permissions;
        END CASE;

        RETURN result;
    END;
$$;
ALTER FUNCTION get_child_permissions(child_permissions allocationpermission[], parent_permissions allocationpermission[], the_grantee grantee, the_grantor grantor) OWNER TO postgres;

-- Query for the allocation permissions themselves, using a business ID. This is to support custom business permissions.
-- If a business has custom permissions, those permissions will be returned. Otherwise, the default will be returned.
CREATE OR REPLACE FUNCTION allocation_role_permissions_for_business(in_business_id UUID)
    RETURNS TABLE (business_id UUID, role_name VARCHAR, permissions allocationpermission[])
    LANGUAGE plpgsql
    STABLE
AS
$$
    BEGIN
        RETURN QUERY SELECT DISTINCT ON (role_name)
            definition.business_id, definition.role_name, definition.permissions
        FROM allocation_role_permissions definition
        WHERE (definition.business_id = in_business_id OR definition.business_id IS NULL)
        ORDER BY definition.role_name ASC, definition.business_id DESC NULLS LAST;
    END;
$$;
ALTER FUNCTION allocation_role_permissions_for_business(in_business_id UUID) OWNER TO postgres;

-- ####################################################################################################################################################################
-- SECTION 5 = ALL PERMISSIONS FUNCTIONS
-- These are the "main" functions of this file. It all stems from one that is intended to return all the allocations and permissions for a business that a user has access to, along with
-- the exact permissions they have on that allocation. Then other functions mix that one in unique and intriguing ways.
-- ####################################################################################################################################################################

-- Get all the allocation permissions and global permissions for a given user. If a Business ID is provided, all allocations within the
-- business that the user has access to will be returned. If an Allocation ID is provided (whether or not there is a Business ID) then
-- only the result for that one allocation (if available) is returned. If no allocation permissions exist, but global permissions exist,
-- then a record with nulls for all the allocation values is returned with the global permissions filled out.
CREATE OR REPLACE FUNCTION get_all_allocation_permissions(in_business_id UUID, in_user_id UUID, in_allocation_id UUID, global_roles VARCHAR[])
    RETURNS TABLE (
        user_id UUID, business_id UUID, user_type VARCHAR, first_name BYTEA, last_name BYTEA,
        allocation_id UUID, parent_allocation_id UUID, ordinal INT, role_name VARCHAR, inherited BOOLEAN, permissions allocationpermission[], global_permissions globaluserpermission[]
    )
    LANGUAGE plpgsql
    STABLE
AS
$$
    DECLARE
        the_global_permissions globaluserpermission[];
        business_status_suspended VARCHAR = 'SUSPENDED';
        the_grantee grantee;
        the_grantor grantor;
        global_permission_cross_business_boundary globaluserpermission = 'CROSS_BUSINESS_BOUNDARY';
        has_cross_business_permission BOOLEAN;
    BEGIN
        SELECT get_global_permissions(in_user_id, global_roles)
        INTO the_global_permissions;

        SELECT the_global_permissions @> ARRAY[global_permission_cross_business_boundary]
        INTO has_cross_business_permission;

        SELECT users.id, users.type, business.status,
            users.first_name_encrypted, users.last_name_encrypted,
            business.id, has_cross_business_permission
        INTO the_grantee
        FROM users
        JOIN business ON users.business_id = business.id
        WHERE users.id = in_user_id
        AND business.status != business_status_suspended
        AND users.archived = FALSE;

        SELECT business.id, business.status
        INTO the_grantor
        FROM business
        WHERE (
            (in_business_id IS NOT NULL AND business.id = in_business_id)
            -- If in_business_id is null, then in_allocation_id must not be null
            -- due to above if check
            OR business.id = (
                SELECT allocation.business_id
                FROM allocation
                WHERE allocation.id = in_allocation_id
            )
        )
        AND business.status != business_status_suspended;

        RETURN QUERY WITH RECURSIVE permissions_hierarchy(
            user_id, business_id, user_type, first_name, last_name,
            allocation_id, parent_allocation_id, ordinal, role_name, inherited, permissions, global_permissions
        ) AS (
            SELECT the_grantee.user_id, the_grantor.business_id, the_grantee.user_type, the_grantee.first_name, the_grantee.last_name,
                root.id, NULL::UUID, 1, get_root_role_name(root_definition.permissions, root_definition.role_name, the_grantee, the_grantor), FALSE,
                get_root_permissions(root_definition.permissions, the_grantee, the_grantor), the_global_permissions
            FROM allocation root
            LEFT JOIN user_allocation_role root_assignment ON root.id = root_assignment.allocation_id AND root_assignment.user_id = the_grantee.user_id
            LEFT JOIN allocation_role_permissions_for_business(the_grantor.business_id) root_definition ON root_assignment.role = root_definition.role_name
            WHERE root.business_id = the_grantor.business_id
            AND root.parent_allocation_id IS NULL
            UNION ALL
            SELECT the_grantee.user_id, the_grantor.business_id, the_grantee.user_type, the_grantee.first_name, the_grantee.last_name,
                walker.id, recurse.allocation_id, recurse.ordinal + 1,
                get_child_role_name(definition.permissions, recurse.permissions, definition.role_name, recurse.role_name, the_grantee, the_grantor),
                do_inherit_permissions(definition.permissions, recurse.permissions),
                get_child_permissions(definition.permissions, recurse.permissions, the_grantee, the_grantor), the_global_permissions
            FROM allocation walker
            JOIN permissions_hierarchy recurse ON recurse.allocation_id = walker.parent_allocation_id
            LEFT JOIN user_allocation_role assignment ON walker.id = assignment.allocation_id AND assignment.user_id = the_grantee.user_id
            LEFT JOIN allocation_role_permissions_for_business(the_grantor.business_id) definition ON assignment.role = definition.role_name
        )
        SELECT hierarchy.*
        FROM permissions_hierarchy hierarchy
        -- This where clause must be here and in the subquery of the union below
        WHERE hierarchy.permissions IS NOT NULL AND array_length(hierarchy.permissions, 1) > 0
        AND the_grantee.user_id IS NOT NULL AND the_grantor.business_id IS NOT NULL
        AND has_access_to_business(the_grantee, the_grantor) = TRUE
        AND (in_allocation_id IS NULL OR hierarchy.allocation_id = in_allocation_id)
        UNION
        SELECT the_grantee.user_id, the_grantor.business_id, the_grantee.user_type, the_grantee.first_name, the_grantee.last_name,
            NULL::UUID, NULL::UUID, 0, NULL::VARCHAR, FALSE, ARRAY[]::allocationpermission[], the_global_permissions
        WHERE (
            SELECT COUNT(*)
            FROM permissions_hierarchy hierarchy
            -- This where clause must be here and in the union above
            WHERE hierarchy.permissions IS NOT NULL AND array_length(hierarchy.permissions, 1) > 0
            AND the_grantee.user_id IS NOT NULL AND the_grantor.business_id IS NOT NULL
            AND has_access_to_business(the_grantee, the_grantor) = TRUE
            AND (in_allocation_id IS NULL OR hierarchy.allocation_id = in_allocation_id)
        ) = 0
        AND array_length(the_global_permissions, 1) > 0
        ORDER BY ordinal, allocation_id ASC;
    END;
$$;
ALTER FUNCTION get_all_allocation_permissions(in_business_id UUID, in_user_id UUID, in_allocation_id UUID, global_roles VARCHAR[]) OWNER TO postgres;

-- Gets all allocation permissions for all users within a business. If an Allocation ID is provided, then only results for that allocation will be returned
-- Does this by cartesian joining the main function with the users table.
CREATE OR REPLACE FUNCTION get_all_allocation_permissions_for_all_users(in_business_id UUID, in_allocation_id UUID)
    RETURNS TABLE (
        user_id UUID, business_id UUID, user_type VARCHAR, first_name BYTEA, last_name BYTEA,
        allocation_id UUID, parent_allocation_id UUID, ordinal INT, role_name VARCHAR, inherited BOOLEAN, permissions allocationpermission[], global_permissions globaluserpermission[]
    )
    LANGUAGE plpgsql
    STABLE
AS
$$
    BEGIN
        IF in_business_id IS NULL THEN
            RAISE EXCEPTION 'Cannot query for permissions for all users within a business without a Business ID';
        END IF;

        RETURN QUERY SELECT permissions.*
        FROM users
        JOIN get_all_allocation_permissions(in_business_id, users.id, in_allocation_id, ARRAY[]::VARCHAR[]) permissions ON permissions.user_id = users.id
        WHERE users.business_id = in_business_id
        ORDER BY permissions.user_id, permissions.ordinal, permissions.allocation_id ASC;
    END;
$$;
ALTER FUNCTION get_all_allocation_permissions_for_all_users(in_business_id UUID, in_allocaton_id UUID) OWNER TO postgres;

-- Get all permissions for all allocations within a business for the specified user.
CREATE OR REPLACE FUNCTION get_all_allocation_permissions_for_business(in_business_id UUID, in_user_id UUID, global_roles VARCHAR[])
    RETURNS TABLE (
        user_id UUID, business_id UUID, user_type VARCHAR, first_name BYTEA, last_name BYTEA,
        allocation_id UUID, parent_allocation_id UUID, ordinal INT, role_name VARCHAR, inherited BOOLEAN, permissions allocationpermission[], global_permissions globaluserpermission[]
    )
    LANGUAGE plpgsql
    STABLE
AS
$$
    BEGIN
        RETURN QUERY SELECT *
        FROM get_all_allocation_permissions(in_business_id, in_user_id, null, global_roles);
    END;
$$;
ALTER FUNCTION get_all_allocation_permissions_for_business(in_business_id UUID, in_user_id UUID, global_roles VARCHAR[]) OWNER TO postgres;

-- Get all permissions for the specified user at the specified allocation.
CREATE OR REPLACE FUNCTION get_all_allocation_permissions_for_allocation(in_allocation_id UUID, in_user_id UUID, global_roles VARCHAR[])
    RETURNS TABLE (
        user_id UUID, business_id UUID, user_type VARCHAR, first_name BYTEA, last_name BYTEA,
        allocation_id UUID, parent_allocation_id UUID, ordinal INT, role_name VARCHAR, inherited BOOLEAN, permissions allocationpermission[], global_permissions globaluserpermission[]
    )
    LANGUAGE plpgsql
    STABLE
AS
$$
    BEGIN
        RETURN QUERY SELECT *
        FROM get_all_allocation_permissions(null, in_user_id, in_allocation_id, global_roles);
    END;
$$;
ALTER FUNCTION get_all_allocation_permissions_for_allocation(in_allocation_id UUID, in_user_id UUID, global_roles VARCHAR[]) OWNER TO postgres;

-- This takes in the same arguments as the function to get all permissions, however it also takes in a specific permission. This is used to evaluate
-- if a user has a specific permission, and will return all the unique allocation IDs for which this is true. This function is used to help
-- filter other SQL queries to ensure only records a user has access to are returned.
CREATE OR REPLACE FUNCTION get_allocation_permissions(in_business_id UUID, in_user_id UUID, global_roles VARCHAR[], permission allocationpermission)
    RETURNS TABLE (allocation_id UUID)
    LANGUAGE SQL
    STABLE
AS
$$
    SELECT DISTINCT hierarchy.allocation_id
    FROM get_all_allocation_permissions(in_business_id, in_user_id,  null, global_roles) hierarchy
    WHERE hierarchy.permissions @> ARRAY[permission]::allocationpermission[];
$$;
ALTER FUNCTION get_allocation_permissions(in_business_id UUID, in_user_id UUID, global_roles VARCHAR[], permission allocationpermission) OWNER TO postgres;

-- Returns a boolean to indicate whether or not a user has a specified global permission
CREATE OR REPLACE FUNCTION has_global_permission(in_user_id UUID, global_roles VARCHAR[], permission globaluserpermission)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
    STABLE
AS
$$
    BEGIN
        RETURN get_global_permissions(in_user_id, global_roles) @> ARRAY[permission]::globaluserpermission[];
    END;
$$;
ALTER FUNCTION has_global_permission(in_user_id UUID, global_roles VARCHAR[], permission globaluserpermission) OWNER TO postgres;