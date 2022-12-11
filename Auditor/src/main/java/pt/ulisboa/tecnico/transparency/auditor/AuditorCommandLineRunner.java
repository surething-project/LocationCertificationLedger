package pt.ulisboa.tecnico.transparency.auditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.Certificate;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AuditorCommandLineRunner implements CommandLineRunner {

  public static Logger logger = LoggerFactory.getLogger(AuditorCommandLineRunner.class);

  private final RestTemplate restTemplate;

  @Value("${ledger.url}")
  private String ledgerURL;

  @Autowired
  public AuditorCommandLineRunner(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public void run(String... args) {
    try {
      Certificate certificate =
          this.restTemplate.
                  getForEntity(ledgerURL + "/certificate", Certificate.class).getBody();
      Path path = Path.of("./src/main/resources/Ledger.crt");

      if (certificate == null) {
        logger.error("Invalid Ledger Certificate!");
        return;
      }
      Files.write(path, certificate.getCertificate().toByteArray());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
