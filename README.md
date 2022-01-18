# To get started

1. install Java 17, Gradle, docker, docker-compose
(see also [Technical Concept Page](https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2031353861/Technical+Concept+Page))
2. `sudo usermod -a -G docker $username`
3. If you're running selinux,
`sudo setfacl -m $username:6 /var/run/docker.sock`
4. Log in again in to pick up the new permissions.
5. `./gradlew jibDockerBuild`
6. Start the services:
   - `docker-compose up` will start the databases (ours and 2 for [Fusion Auth](https://fusionauth.io)),
and you'll want to run com.clearspend.capital.CapitalApplication with your IDE.
   - `docker-compose --profile mon up` will also start [Prometheus](https://prometheus.io/) and [Grafana](https://grafana.com/) to monitor this service running in your IDE.
   - `docker-compose --profile ui up` will also start UI service in a container pointing to the application running in your IDE
   To make it happen - checkout the latest capital-ui sources to the ../capital-ui folder and then use `docker-compose --profile ui build`
   - [com.clearspend.capital.CapitalApplication](src/main/java/com/clearspend/capital/CapitalApplication.java) is the main class
7. [Spring actuator](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#actuator) status check: `curl http://localhost:8080/actuator/health`

See also [our schema](src/main/resources/db/migration/V1_0__Baseline.sql) which is managed by [flyway](https://flywaydb.org).

Service urls and accounts:
- Fusion Auth: http://127.0.0.1:9011 admin@clearspend.com/admin
- Prometheus: http://127.0.0.1:9090
- Grafana: http://127.0.0.1:3000 admin@clearspend.com/admin

# To use Plaid features

To avoid publishing secrets to a git repository, the plaid secret is 
held in an env variable named `CLIENT_PLAID_SECRET`. Plaid features are
<u>not required</u> to run the rest of the app, so it is not
necessary to include it if you don't plan on linking bank accounts. 
If you do wish to use plaid features, populate the variable. This value can
be found in the Plaid dashboard.

# Business Onboarding

Sequence of APIs:

1. Create Business and Business Owner
    - POST `/business-prospects`
    - POST `/business-prospects/{businessProspectId}/validate-identifier` (email)
    - POST `/business-prospects/{businessProspectId}/phone`
    - POST `/business-prospects/{businessProspectId}/validate-identifier` (phone)
    - POST `/business-prospects/{businessProspectId}/password`
    - POST `/business-prospects/{businessProspectId}/convert`
    - POST `/business-owners`
    - PUT `/business-owners/{businessOwnerId}`
3. Link Bank Account. All business-bank-accounts calls require businessId to be passed in a header
   until we have JWTs
    - GET `/business-bank-accounts/link-token`
    - GET `/business-bank-accounts/link-token/{linkToken}/accounts`
4. Deposit to or withdraw from a Business account to a bank account
    - GET `/business-bank-accounts`
    - POST `/business-bank-accounts/{businessBankAccountId}/transactions`
5. Retrieve Business Account information
   - POST `/businesses/accounts` (get business account)
6. Create Allocation
    - GET `/programs`
    - POST `/allocations`
7. Traverse allocations
    - GET `/allocations/{allocationId}` (get one)
    - GET `/businesses/allocations` (get root Allocations)
    - GET `/allocations/{allocationId}/children` (get children of an allocation)
8. Reallocate funds
    - POST `/businesses/transactions` (Business <-> Allocation)
    - POST `/allocations/{allocationId}/transactions` (Allocation <-> Card)

# SendGrid

This repo contains samples of dynamic templates: https://github.com/sendgrid/email-templates
