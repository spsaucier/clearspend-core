# To get started

`./gradlew jibDockerBuild`

`docker compose --profile full up`

#To use Plaid features
To avoid publishing secrets to a git repository, the plaid secret is currently held in an env varable named 
`PLAID_SECRET`. Plaid features are <u>not required</u> to run the rest of the app so it is not necessary to 
include it if you don't plan on linking bank accounts. If you do wish to use plaid features, place the plaid
secret key in an env var named `PLAID_SECRET`. This value can be found in the Plaid dashboard.

# Business Onboarding

Sequence of APIs:

- POST `/business-prospect`
- POST `/business-prospect/{businessProspectId}/validate-identifier` (email)
- POST `/business-prospect/{businessProspectId}/phone`
- POST `/business-prospect/{businessProspectId}/validate-identifier` (phone)
- POST `/business-prospect/{businessProspectId}/password`
- POST `/business-prospect/{businessProspectId}/convert`
