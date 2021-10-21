package com.tranwall.capital.client.twilio;

import com.twilio.http.HttpClient;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
    RequestConfig config =
        RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(30500).build();

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultMaxPerRoute(10);
    connectionManager.setMaxTotal(10 * 2);

    HttpHost proxy = new HttpHost(proxyHost, proxyPort);

    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }

            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
              System.out.println();
            }

            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
              System.out.println();
            }
          }
        };
    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpClientBuilder httpClientBuilder =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setProxy(proxy)
            .setSSLContext(sslContext)
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setDefaultRequestConfig(config);

    this.httpClient = new NetworkHttpClient(httpClientBuilder);
    this.httpClient =
        new NetworkHttpClient(
            HttpClients.custom()
                .setProxy(proxy)
                .setSSLContext(
                    new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
    /*
        HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
        customRequestFactory.setHttpClient(httpClientBuilder);
        return builder.requestFactory(() -> customRequestFactory).build();




        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder
            .setConnectionManager(connectionManager)
            .setProxy(proxy)
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setDefaultRequestConfig(config);

        // Inclusion of Twilio headers and build() is executed under this constructor
        this.httpClient = new NetworkHttpClient(clientBuilder);
    */
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
