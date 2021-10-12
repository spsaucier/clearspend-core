# To get started

`./gradlew jibDockerBuild`

`docker compose --profile full up`

# To use Plaid features

To avoid publishing secrets to a git repository, the plaid secret is currently held in an env
varable named
`PLAID_SECRET`. Plaid features are <u>not required</u> to run the rest of the app so it is not
necessary to include it if you don't plan on linking bank accounts. If you do wish to use plaid
features, place the plaid secret key in an env var named `PLAID_SECRET`. This value can be found in
the Plaid dashboard.

# Business Onboarding

Sequence of APIs:

1. Create Business and Business Owner
    - POST `/business-prospects`
    - POST `/business-prospects/{businessProspectId}/validate-identifier` (email)
    - POST `/business-prospects/{businessProspectId}/phone`
    - POST `/business-prospects/{businessProspectId}/validate-identifier` (phone)
    - POST `/business-prospects/{businessProspectId}/password`
    - POST `/business-prospects/{businessProspectId}/convert`
    - PUT `/business-owners/{businessOwnerId}`
2. Link Bank Account. All business-bank-accounts calls require businessId to be passed in a header
   until we have JWTs
    - GET `/business-bank-accounts/link-token/{businessId}`
    - GET `/business-bank-accounts/{linkToken}` or GET `/business-bank-accounts/{linkToken}` or
    - POST `/business-bank-accounts/{businessBankAccountId}/transactions`
3. Create Allocation
    - GET `/programs`
    - POST `/allocations`