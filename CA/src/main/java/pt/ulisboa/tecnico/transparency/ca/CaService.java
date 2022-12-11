package pt.ulisboa.tecnico.transparency.ca;

import com.google.protobuf.ByteString;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.transparency.ca.contract.CAOuterClass.Certificate;
import pt.ulisboa.tecnico.transparency.ca.contract.CAOuterClass.CertificateSigningRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service
public class CaService {

  public static Logger logger = LoggerFactory.getLogger(CaService.class);

  public Certificate generateCertificate(CertificateSigningRequest csr) {
    byte[] csrByteArray = csr.getCertificateSigningRequest().toByteArray();
    try {
      // Create temporary files
      File csrFile = Files.createTempFile(csr.getName(), ".csr").toFile();
      File crtFile = Files.createTempFile(csr.getName(), ".crt").toFile();

      FileOutputStream outputStream = new FileOutputStream(csrFile.getAbsolutePath());
      outputStream.write(csrByteArray);

      URL resourcePath = Objects.requireNonNull(this.getClass().getResource("/signCSR.sh"));
      CommandLine cmd = new CommandLine(resourcePath.getPath());
      cmd.addArgument(csrFile.getAbsolutePath());
      cmd.addArgument(crtFile.getAbsolutePath());
      if (new DefaultExecutor().execute(cmd) == 0) {
        logger.info("CRT for " + csr.getName() + " Created!");
      } else {
        logger.error("CRT for " + csr.getName() + " Not Created!");
      }

      byte[] crtBytes = Files.readAllBytes(Path.of(crtFile.getAbsolutePath()));
      return Certificate.newBuilder().setCertificate(ByteString.copyFrom(crtBytes)).build();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
