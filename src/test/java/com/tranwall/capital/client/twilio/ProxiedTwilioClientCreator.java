package com.tranwall.capital.client.twilio;

import com.twilio.http.HttpClient;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

// from: https://www.twilio.com/docs/libraries/java/custom-http-clients-java
public class ProxiedTwilioClientCreator {

  private String username;
  private String password;
  private String proxyHost;
  private int proxyPort;
  private HttpClient httpClient;

  /**
   * Constructor for ProxiedTwilioClientCreator
   *
   * @param username
   * @param password
   * @param proxyHost
   * @param proxyPort
   */
  public ProxiedTwilioClientCreator(
      String username, String password, String proxyHost, int proxyPort) {
    this.username = username;
    this.password = password;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
  }

  /**
   * Creates a custom HttpClient based on default config as seen on: {@link
   * com.twilio.http.NetworkHttpClient#NetworkHttpClient() constructor}
   */
  @SneakyThrows
  private void createHttpClient() {
    HttpHost proxy = new HttpHost(proxyHost, proxyPort);
    this.httpClient =
        new NetworkHttpClient(
            HttpClients.custom()
                .setProxy(proxy)
                .setSSLContext(
                    new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
  }

  /**
   * Get the custom client or builds a new one
   *
   * @return a TwilioRestClient object
   */
  public TwilioRestClient getClient() {
    if (this.httpClient == null) {
      this.createHttpClient();
    }

    TwilioRestClient.Builder builder = new TwilioRestClient.Builder(username, password);
    return builder.httpClient(this.httpClient).build();
  }
}
