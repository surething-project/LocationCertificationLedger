package pt.ulisboa.tecnico.transparency.certificaterequester;

import com.google.protobuf.ByteString;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pt.ulisboa.tecnico.transparency.ca.contract.CAOuterClass.Certificate;
import pt.ulisboa.tecnico.transparency.ca.contract.CAOuterClass.CertificateSigningRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RequesterCommandLineRunner implements CommandLineRunner {

  public static Logger logger = LoggerFactory.getLogger(RequesterCommandLineRunner.class);

  @Value("${requester.name:null}")
  private String name;

  @Value("${ca.url}")
  private String caURL;

  @Autowired private RestTemplate restTemplate;

  public RequesterCommandLineRunner(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public void run(String... args) {
    try {
      DefaultExecutor executor = new DefaultExecutor();
      URL resourceURl = this.getClass().getResource("/");
      if (resourceURl == null) {
        logger.error("Failed Getting Resource Path");
        return;
      }

      // Create CSR
      CommandLine cmd = new CommandLine(resourceURl.getPath() + "/newCSR.sh");
      cmd.addArgument(this.name);
      if (executor.execute(cmd) == 0) {
        logger.info("Success on Creating the Key and the CSR for " + this.name + "!");
      } else {
        logger.error("Failed Creating the Key and the CSR for " + this.name + "!");
        return;
      }

      // Request Certificate
      String requesterPath = String.format("../%s/src/main/resources/%s", this.name, this.name);
      Certificate certificate = getCertificate(Files.readAllBytes(Path.of(requesterPath + ".csr")));
      // Write Certificate
      File cert = new File(requesterPath + ".crt");
      FileOutputStream outputStream = new FileOutputStream(cert);
      outputStream.write(certificate.getCertificate().toByteArray());

      // Create P12 Key
      cmd = new CommandLine(resourceURl.getPath() + "/newKeyStore.sh");
      cmd.addArgument(this.name);
      if (executor.execute(cmd) == 0) {
        logger.info("Success on Creating the P12 Key for" + this.name + "!");
      } else {
        logger.info("Failed Creating the P12 Key for " + this.name + "!");
      }
    } catch (IOException e) {
      logger.error(e.getMessage());
    }
  }

  private Certificate getCertificate(byte[] csrByteArray) {
    CertificateSigningRequest certificateSigningRequest =
        CertificateSigningRequest.newBuilder()
            .setCertificateSigningRequest(ByteString.copyFrom(csrByteArray))
            .setName(this.name)
            .build();
    return this.restTemplate.postForObject(
        this.caURL + "/ca", certificateSigningRequest, Certificate.class);
  }
}
