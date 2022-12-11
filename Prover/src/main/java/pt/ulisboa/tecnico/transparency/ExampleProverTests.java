package pt.ulisboa.tecnico.transparency;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import eu.surething_project.core.*;
import eu.surething_project.core.wi_fi.WiFiNetworksEvidence;
import eu.surething_project.signature.util.SignatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.AuditResult;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SLCT;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SignedLocationCertificate;
import pt.ulisboa.tecnico.transparency.monitor.contract.Monitor.SignedLCs;
import pt.ulisboa.tecnico.transparency.verifier.contract.UserOuterClass.Authorization;
import pt.ulisboa.tecnico.transparency.verifier.contract.UserOuterClass.Credentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.protobuf.util.Timestamps.fromMillis;

public class ExampleProverTests {
  public static Logger logger = LoggerFactory.getLogger(ExampleProverTests.class);
  private final PrivateKey privateKey;
  private final RestTemplate restTemplate;

  public String verifierURL;
  public String monitorURL;
  public String auditorURL;
  public String ledgerURL;

  public ExampleProverTests(PrivateKey privateKey) throws IOException {
    this.privateKey = privateKey;
    Properties appProps = new Properties();
    String path =
        Objects.requireNonNull(this.getClass().getResource("/application.properties")).getPath();
    appProps.load(new FileInputStream(path));
    this.verifierURL = appProps.getProperty("url.verifier");
    this.monitorURL = appProps.getProperty("url.monitor");
    this.auditorURL = appProps.getProperty("url.auditor");
    this.ledgerURL = appProps.getProperty("url.ledger");
    this.restTemplate = new RestTemplate(List.of(new ProtobufHttpMessageConverter()));
  }

  private static SignedLocationClaim signLocationClaim(
      PrivateKey privateKey, LocationClaim locationClaim) throws Exception {
    byte[] signatureArray = SignatureManager.sign(locationClaim.toByteArray(), privateKey);

    Random rand = new Random();
    long randomLong = rand.nextLong();

    Signature signature =
        Signature.newBuilder()
            .setCryptoAlgo("SHA256withRSA")
            .setValue(ByteString.copyFrom(signatureArray))
            .setNonce(randomLong)
            .build();

    return SignedLocationClaim.newBuilder()
        .setClaim(locationClaim)
        .setProverSignature(signature)
        .build();
  }

  private static LocationClaim createLocationClaim(
      String claimId,
      double latitude,
      double longitude,
      Timestamp ts,
      String wifiId,
      String ssid,
      String rssi) {
    return LocationClaim.newBuilder()
        .setClaimId(claimId)
        .setProverId(claimId)
        .setLocation(
            Location.newBuilder()
                .setLatLng(
                    LatLng.newBuilder().setLatitude(latitude).setLongitude(longitude).build())
                .build())
        .setTime(Time.newBuilder().setTimestamp(ts).build())
        .setEvidenceType("eu.project.core.wi_fi.WiFiNetworksEvidence")
        .setEvidence(
            Any.pack(
                WiFiNetworksEvidence.newBuilder()
                    .setId(wifiId)
                    .addAps(
                        WiFiNetworksEvidence.AP.newBuilder().setSsid(ssid).setRssi(rssi).build())
                    .build()))
        .build();
  }

  private static HttpHeaders getHTTPHeader(String jwt) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", jwt);
    return headers;
  }

  public void testLCNoAuthVerifier() {
    logger.info("Testing Authentication of Verifier!");
    try {
      LocationClaim locationClaim =
          createLocationClaim("1", 53.3, 85.3, fromMillis(1), "ABC", "ssid-A", "-89");
      SignedLocationClaim signedLClaim = signLocationClaim(privateKey, locationClaim);
      this.restTemplate.postForObject(
          this.verifierURL + "/verifier", signedLClaim, SignedLocationClaim.class);
    } catch (Exception e) {
      logger.info("Exception expected! Authentication Token Needed: " + e.getMessage());
    }
  }

  public String testUserVerifier() {
    logger.info("Testing User Endpoint of Verifier!");
    Credentials credentials =
        Credentials.newBuilder().setUsername("Rafael").setPassword("Fig").build();
    try {
      // Register User
      Authorization authorization =
          this.restTemplate.postForObject(
              this.verifierURL + "/user/register", credentials, Authorization.class);
      assert authorization != null;
      logger.info(
          String.format(
              "Username: %s has jwt %s", credentials.getUsername(), authorization.getJwt()));
    } catch (RuntimeException e) {
      logger.info(e.getMessage());
    }
    Authorization authorization =
        this.restTemplate.postForObject(
            this.verifierURL + "/user/login", credentials, Authorization.class);
    assert authorization != null;
    logger.info(
        String.format(
            "Username: %s has jwt %s", credentials.getUsername(), authorization.getJwt()));
    return authorization.getJwt();
  }

  public SLCT testLCVerifier(String jwt) throws Exception {
    logger.info("Testing Submission of Location Claims!");
    LocationClaim locationClaim;
    Random random = new Random();
    for (int i = 0; i < 1; i++) {
      for (int j = 0; j < 10; j++) {
        locationClaim =
            createLocationClaim(
                String.valueOf(j + 1),
                random.nextFloat(),
                random.nextFloat(),
                fromMillis(System.currentTimeMillis()),
                UUID.randomUUID().toString(),
                "ssid-" + j + 1,
                String.valueOf(random.nextFloat()));
        sendLocationClaim(locationClaim, jwt);
      }
    }

    locationClaim = createLocationClaim("0", 53.3, 85.3, fromMillis(1), "ABCD", "ssid-B", "-89");
    sendLocationClaim(locationClaim, jwt);

    locationClaim = createLocationClaim("0", 0, 0, fromMillis(1), "FGH", "ssid-B", "-1");
    SLCT slct = sendLocationClaim(locationClaim, jwt);
    logger.info("Last SLCT! Signed LCId = " + slct.getSignedLocationCertificate().getId());
    logger.info("Waiting for MMD (30s)");
    TimeUnit.SECONDS.sleep(30);
    return slct;
  }

  public void testAuditor(String jwt, SLCT slct) {
    logger.info("Testing Audit!");
    AuditResult auditResultProto =
        this.restTemplate.postForObject(
            this.auditorURL + "/auditor",
            new HttpEntity<>(slct, getHTTPHeader(jwt)),
            AuditResult.class);
    logger.info("AuditResult Received: " + auditResultProto);
  }

  public void testMonitor(String jwt) throws URISyntaxException {
    logger.info("Testing Monitor /verifierId, id = 1");
    String url = String.format("%s/monitor/verifierId?verifierId=%d", this.monitorURL, 1);
    SignedLCs signedLCs =
        this.restTemplate
            .exchange(
                RequestEntity.get(new URI(url)).headers(getHTTPHeader(jwt)).build(),
                SignedLCs.class)
            .getBody();
    assert signedLCs != null;
    monitorSearchLog(signedLCs);

    logger.info("Testing Monitor /evidenceType, type = eu.project.core.wi_fi.WiFiNetworksEvidence");
    url =
        String.format(
            "%s/monitor/evidenceType?evidenceType=eu.project.core.wi_fi.WiFiNetworksEvidence",
            this.monitorURL);
    signedLCs =
        this.restTemplate
            .exchange(
                RequestEntity.get(new URI(url)).headers(getHTTPHeader(jwt)).build(),
                SignedLCs.class)
            .getBody();
    assert signedLCs != null;
    monitorSearchLog(signedLCs);

    logger.info("Testing Monitor /time, time = 1ms");
    url = String.format("%s/monitor/time", this.monitorURL);
    Time time = Time.newBuilder().setTimestamp(fromMillis(1)).build();
    signedLCs =
        this.restTemplate.postForObject(
            url, new HttpEntity<>(time, getHTTPHeader(jwt)), SignedLCs.class);
    assert signedLCs != null;
    monitorSearchLog(signedLCs);
  }

  private void monitorSearchLog(SignedLCs signedLCs) {
    logger.info("Size Of Answer:" + signedLCs.getSignedLocationCertificateCount());
    for (int i = 0; i < signedLCs.getSignedLocationCertificateCount(); i++) {
      SignedLocationCertificate signedLC = signedLCs.getSignedLocationCertificate(i);
      logger.info("SignedCertificateId" + i + ": " + signedLC.getId());
    }
  }

  private SLCT sendLocationClaim(LocationClaim locationClaim, String jwt) throws Exception {
    SignedLocationClaim signedLClaim = signLocationClaim(this.privateKey, locationClaim);
    SLCT slct =
        this.restTemplate.postForObject(
            this.verifierURL + "/verifier",
            new HttpEntity<>(signedLClaim, getHTTPHeader(jwt)),
            SLCT.class);
    logger.info("Received a SLCT!");
    return slct;
  }
}
