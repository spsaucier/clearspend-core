package com.tranwall.capital;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.model.RegexBody;
import org.mockserver.verify.VerificationTimes;

@RequiredArgsConstructor
public class MockServerHelper {

  @Nonnull private final MockServerClient mockServerClient;

  public void verifyEmailVerificationCalled(int times) {
    mockServerClient.verify(
        request()
            .withPath("/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/Verifications")
            .withBody(RegexBody.regex(".*Channel=email.*")),
        VerificationTimes.exactly(times));
  }

  public void verifyPhoneVerificationCalled(int times) {
    mockServerClient.verify(
        request()
            .withPath("/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/Verifications")
            .withBody(RegexBody.regex(".*Channel=sms.*")),
        VerificationTimes.exactly(times));
  }

  public void expectOtpViaEmail() {
    mockServerClient
        .when(
            request()
                .withMethod("POST")
                .withPath("/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/Verifications")
                .withBody(RegexBody.regex(".*Channel=email.*")),
            // TODO this should be able to check of the request is actually for email
            // but I haven't managed to write correct JsonPath expression for
            //  {
            //    "method" : "POST",
            //    "path" : "/v2/Services/xyu/Verifications",
            //    "headers" : {
            //      "Authorization" : [ "Basic xyu" ],
            //      "Content-Type" : [ "application/x-www-form-urlencoded" ],
            //      "X-Twilio-Client" : [ "" ],
            //      "User-Agent" : [ "" ],
            //      "Accept" : [ "application/json" ],
            //      "Accept-Encoding" : [ "utf-8" ],
            //      "Host" : [ "example.com" ],
            //      "Connection" : [ "Keep-Alive" ],
            //      "content-length" : [ "68" ]
            //    },
            //    "keepAlive" : true,
            //    "secure" : true,
            //    "body" : "Channel=email&To=fuuuuck@example.com"
            //  }
            // .withBody(JsonPathBody.jsonPath("$.body")),
            Times.exactly(1),
            TimeToLive.exactly(TimeUnit.SECONDS, 60L),
            10)
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    """
                    {
                        "status": "pending",
                        "payee": null,
                        "date_updated": "2021-10-18T14:00:28Z",
                        "send_code_attempts": [
                            {
                                "attempt_sid": null,
                                "channel_id": "UMiPno7rTfex8esP3ihDWQ",
                                "channel": "email",
                                "time": "2021-10-18T14:00:28.368Z"
                            }
                        ],
                        "account_sid": "AC08834a8aab6f817ea96ae7149e8befff",
                        "to": "pavel.petrovichev@tranwall.com",
                        "amount": null,
                        "valid": false,
                        "lookup": {
                            "carrier": {
                                "mobile_country_code": null,
                                "type": null,
                                "error_code": null,
                                "mobile_network_code": null,
                                "name": null
                            }
                        },
                        "url": "https://verify.twilio.com/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/Verifications/VE1e7c90bd86786d1dca314fedeced2548",
                        "sid": "VE1e7c90bd86786d1dca314fedeced2548",
                        "date_created": "2021-10-18T14:00:28Z",
                        "service_sid": "VAbaf002bd328d70c5aadf45f92d8c49ac",
                        "channel": "email"
                    }
                    """));
  }

  public void expectOtpViaSms() {
    mockServerClient
        .when(
            request()
                .withMethod("POST")
                .withPath("/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/Verifications")
                .withBody(RegexBody.regex(".*Channel=sms.*")),
            Times.exactly(1),
            TimeToLive.exactly(TimeUnit.SECONDS, 60L),
            10)
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    """
                    {
                        "status": "pending",
                        "payee": null,
                        "date_updated": "2021-10-18T14:00:28Z",
                        "send_code_attempts": [
                            {
                                "attempt_sid": null,
                                "channel_id": "UMiPno7rTfex8esP3ihDWQ",
                                "channel": "sms",
                                "time": "2021-10-18T14:00:28.368Z"
                            }
                        ],
                        "account_sid": "AC08834a8aab6f817ea96ae7149e8befff",
                        "to": "pavel.petrovichev@tranwall.com",
                        "amount": null,
                        "valid": false,
                        "lookup": {
                            "carrier": {
                                "mobile_country_code": null,
                                "type": null,
                                "error_code": null,
                                "mobile_network_code": null,
                                "name": null
                            }
                        },
                        "url": "https://verify.twilio.com/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/Verifications/VE1e7c90bd86786d1dca314fedeced2548",
                        "sid": "VE1e7c90bd86786d1dca314fedeced2548",
                        "date_created": "2021-10-18T14:00:28Z",
                        "service_sid": "VAbaf002bd328d70c5aadf45f92d8c49ac",
                        "channel": "sms"
                    }
                    """));
  }

  public void expectEmailVerification(String expectedOtp) {
    mockServerClient
        .when(
            request()
                .withMethod("POST")
                .withPath("/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/VerificationCheck")
                .withBody(RegexBody.regex(String.format(".*Code=%s.*", expectedOtp))),
            Times.exactly(1),
            TimeToLive.exactly(TimeUnit.SECONDS, 60L),
            10)
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    """
                    {
                        "status": "approved",
                        "payee": null,
                        "date_updated": "2021-10-19T06:25:26Z",
                        "account_sid": "AC08834a8aab6f817ea96ae7149e8befff",
                        "to": "pavel.petrovichev@tranwall.com",
                        "amount": null,
                        "valid": true,
                        "sid": "VEe9427213de04371a8f099c288a4e2625",
                        "date_created": "2021-10-19T06:21:21Z",
                        "service_sid": "VAbaf002bd328d70c5aadf45f92d8c49ac",
                        "channel": "email"
                    }
                    """));
  }

  public void expectPhoneVerification(String expectedOtp) {
    mockServerClient
        .when(
            request()
                .withMethod("POST")
                .withPath("/v2/Services/VAbaf002bd328d70c5aadf45f92d8c49ac/VerificationCheck")
                .withBody(RegexBody.regex(String.format(".*Code=%s.*", expectedOtp))),
            Times.exactly(1),
            TimeToLive.exactly(TimeUnit.SECONDS, 60L),
            10)
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    """
                    {
                        "status": "approved",
                        "payee": null,
                        "date_updated": "2021-10-19T06:28:44Z",
                        "account_sid": "AC08834a8aab6f817ea96ae7149e8befff",
                        "to": "+79817771583",
                        "amount": null,
                        "valid": true,
                        "sid": "VE8690259b21f8be9cca9e61064e6332cc",
                        "date_created": "2021-10-19T06:27:51Z",
                        "service_sid": "VAbaf002bd328d70c5aadf45f92d8c49ac",
                        "channel": "sms"
                    }
                                  """));
  }

  public void expectFusionAuthCreateUser(@NonNull TypedId<BusinessOwnerId> businessOwnerId) {
    mockServerClient
        .when(
            request().withMethod("POST").withPath("/fusionauth/api/user/.*"),
            Times.exactly(1),
            TimeToLive.exactly(TimeUnit.SECONDS, 60L),
            10)
        .respond(
            response()
                .withStatusCode(200)
                .withBody(
                    """
                    {
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImMxVU5ON0pIUVc4X21ROHBTaWZKbzBXekdybDlTbTRnIn0.eyJleHAiOjE1ODY4ODQzNzksImlhdCI6MTU4Njg4NDMxOSwiaXNzIjoiZnVzaW9uYXV0aC5pbyIsInN1YiI6IjAwMDAwMDAwLTAwMDAtMDAwMS0wMDAwLTAwMDAwMDAwMDAwMCIsImF1dGhlbnRpY2F0aW9uVHlwZSI6IlVTRVJfQ1JFQVRFIiwiZW1haWwiOiJ0ZXN0MEBmdXNpb25hdXRoLmlvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXJuYW1lMCJ9.Z1jV8xDcayZZDBdLRVd2fIyowhstRI4Dgk7_u2XFerc",
                      "user": {
                        "active": true,
                        "breachedPasswordLastCheckedInstant": 1471786483322,
                        "birthDate": "1976-05-30",
                        "connectorId": "e3306678-a53a-4964-9040-1c96f36dda72",
                        "data": {
                          "displayName": "Johnny Boy",
                          "favoriteColors": [
                            "Red",
                            "Blue"
                          ]
                        },
                        "email": "example@fusionauth.io",
                        "expiry": 1571786483322,
                        "firstName": "John",
                        "fullName": "John Doe",
                        "id": "00000000-0000-0001-0000-000000000000",
                        "imageUrl": "http://65.media.tumblr.com/tumblr_l7dbl0MHbU1qz50x3o1_500.png",
                        "lastLoginInstant": 1471786483322,
                        "lastName": "Doe",
                        "memberships": [{
                          "data": {
                            "externalId": "cc6714c6-286c-411c-a6bc-ee413cda1dbc"
                          },
                          "groupId": "2cb5c83f-53ff-4d16-88bd-c5e3802111a5",
                          "id": "27218714-305e-4408-bac0-23e7e1ddceb6",
                          "insertInstant": 1471786482322
                        }],
                        "middleName": "William",
                        "mobilePhone": "303-555-1234",
                        "passwordChangeRequired": false,
                        "passwordLastUpdateInstant": 1471786483322,
                        "preferredLanguages": [
                          "en",
                          "fr"
                        ],
                        "registrations": [
                          {
                            "applicationId": "10000000-0000-0002-0000-000000000001",
                            "data": {
                              "displayName": "Johnny",
                              "favoriteSports": [
                                "Football",
                                "Basketball"
                              ]
                            },
                            "id": "00000000-0000-0002-0000-000000000000",
                            "insertInstant": 1446064706250,
                            "lastLoginInstant": 1456064601291,
                            "preferredLanguages": [
                              "en",
                              "fr"
                            ],
                            "roles": [
                              "user",
                              "community_helper"
                            ],
                            "timezone": "America/Chicago",
                            "username": "johnny123",
                            "usernameStatus": "ACTIVE"
                          }
                        ],
                        "timezone": "America/Denver",
                        "tenantId": "f24aca2b-ce4a-4dad-951a-c9d690e71415",
                        "twoFactor": {
                          "methods": [
                            {
                              "authenticator": {
                                "algorithm": "HmacSHA1",
                                "codeLength": 6,
                                "timeStep": 30
                              },
                              "id": "35VW",
                              "method": "authenticator"
                            },
                            {
                              "id": "V7SH",
                              "method": "sms",
                              "mobilePhone": "555-555-5555"
                            },
                            {
                              "email": "example@fusionauth.io",
                              "id": "7K2G",
                              "method": "email"
                            }
                          ]
                        },
                        "usernameStatus": "ACTIVE",
                        "username": "johnny123",
                        "verified": true
                      }
                    }
                    """));
  }
}
