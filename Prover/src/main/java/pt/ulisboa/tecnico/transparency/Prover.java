package pt.ulisboa.tecnico.transparency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SLCT;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

public class Prover {
  public static Logger logger = LoggerFactory.getLogger(Prover.class);

  public static void main(String[] args) {
    try {
      FileInputStream is = new FileInputStream("./src/main/resources/Prover.p12");
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(is, "Prover".toCharArray());
      PrivateKey privateKey = (PrivateKey) keystore.getKey("ProverCert", "Prover".toCharArray());
      ExampleProverTests tests = new ExampleProverTests(privateKey);
      tests.testLCNoAuthVerifier();
      String jwt = tests.testUserVerifier();
      SLCT slct = tests.testLCVerifier(jwt);
      tests.testAuditor(jwt, slct);
      tests.testMonitor(jwt);
    } catch (Exception e) {
      logger.error("Unexpected Error: " + e.getMessage());
    }
  }
}
