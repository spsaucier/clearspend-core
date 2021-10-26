package com.tranwall.capital;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.model.RegexBody;
import org.mockserver.verify.VerificationTimes;

@RequiredArgsConstructor
public class MockServerHelper {

  @Nonnull private final ClientAndServer mockServerClient;

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
}
