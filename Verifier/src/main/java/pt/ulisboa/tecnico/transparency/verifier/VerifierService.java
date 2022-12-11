package pt.ulisboa.tecnico.transparency.verifier;

import com.google.protobuf.ByteString;
import eu.surething_project.core.LocationCertificate;
import eu.surething_project.core.LocationVerification;
import eu.surething_project.core.Signature;
import eu.surething_project.core.SignedLocationClaim;
import eu.surething_project.signature.util.SignatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SLCT;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Random;

@Service
public class VerifierService {

  public static Logger logger = LoggerFactory.getLogger(VerifierService.class);
  private final PrivateKey privateKey;
  private final RestTemplate restTemplate;

  @Value("${ledger.url}")
  private String ledgerURL;

  @Value("${verifier.id}")
  private int verifierId;

  @Autowired
  public VerifierService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    try {
      FileInputStream is = new FileInputStream("src/main/resources/Verifier.p12");
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(is, "Verifier".toCharArray());
      this.privateKey = (PrivateKey) keystore.getKey("VerifierCert", "Verifier".toCharArray());
    } catch (IOException | GeneralSecurityException e) {
      logger.error("Error While Trying to Get Private Key of Verifier: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public SLCT storeNewSignedLC(SignedLocationClaim signedLClaim) {
    LocationVerification locationVerification =
        LocationVerification.newBuilder()
            .setVerifierId(String.valueOf(verifierId))
            .setClaimId(signedLClaim.getClaim().getClaimId())
            .setTime(signedLClaim.getClaim().getTime())
            .setEvidenceType(signedLClaim.getClaim().getEvidenceType())
            .setEvidence(signedLClaim.getClaim().getEvidence())
            .build();
    byte[] signatureByteArray;
    try {
      signatureByteArray = SignatureManager.sign(locationVerification.toByteArray(), privateKey);
    } catch (Exception e) {
      logger.error("Could Not Sign Location Verification!");
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error While Signing Location Claim!");
    }
    Random random = new Random();
    Signature signature =
        Signature.newBuilder()
            .setCryptoAlgo("SHA256withRSA")
            .setValue(ByteString.copyFrom(signatureByteArray))
            .setNonce(random.nextLong())
            .build();
    LocationCertificate locationCertificate =
        LocationCertificate.newBuilder()
            .setVerification(locationVerification)
            .setVerifierSignature(signature)
            .build();
    SLCT slct = this.restTemplate.postForObject(this.ledgerURL, locationCertificate, SLCT.class);
    logger.info("SLCT: " + slct);
    return slct;
  }
}
