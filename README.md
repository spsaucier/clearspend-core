#To get started
`./gradlew build`

`docker compose --profile full up`

# Business Onboarding
Sequence of APIs:
- POST `/business-prospect`
- POST `/business-prospect/{businessProspectId}/validate-identifier` (email)
- POST `/business-prospect/{businessProspectId}/phone`
- POST `/business-prospect/{businessProspectId}/validate-identifier` (phone)
- POST `/business-prospect/{businessProspectId}/password`
- POST `/business-prospect/{businessProspectId}/convert`