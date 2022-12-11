package pt.ulisboa.tecnico.transparency.monitor;

import eu.surething_project.core.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.ConsistencyProofResult;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.LogContent;
import pt.ulisboa.tecnico.transparency.monitor.contract.Monitor.MTHCheck;
import pt.ulisboa.tecnico.transparency.monitor.contract.Monitor.SignedLCs;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonitorService {

  public static Logger logger = LoggerFactory.getLogger(MonitorService.class);
  private final SignedLCertificateRepository signedLCertificateRepository;
  private final RestTemplate restTemplate;

  @Value("${url.ledger}")
  private String ledgerURL;

  private SignedTreeHead STH;
  private int lastSignedLCIndexSaved = 0;

  @Autowired
  public MonitorService(
      SignedLCertificateRepository signedLCertificateRepository, RestTemplate restTemplate) {
    this.signedLCertificateRepository = signedLCertificateRepository;
    this.restTemplate = restTemplate;
    this.STH = SignedTreeHead.newBuilder().build();
  }

  public SignedTreeHead getSTH() {
    return STH;
  }

  public void setSTH(SignedTreeHead STH) {
    this.STH = STH;
  }

  public SignedLCs getLCsByVerifierId(String verifierId) {
    List<SignedLCertificate> signedLC = signedLCertificateRepository.findByVerifierId(verifierId);
    return SignedLCs.newBuilder()
        .addAllSignedLocationCertificate(
            signedLC.stream().map(SignedLCertificate::toProtobuf).collect(Collectors.toList()))
        .build();
  }

  public SignedLCs getLCsByEvidenceType(String evidenceType) {
    List<SignedLCertificate> signedLC =
        signedLCertificateRepository.findByEvidenceType(evidenceType);
    return SignedLCs.newBuilder()
        .addAllSignedLocationCertificate(
            signedLC.stream().map(SignedLCertificate::toProtobuf).collect(Collectors.toList()))
        .build();
  }

  public SignedLCs getLCsByTime(Time time) {
    List<SignedLCertificate> signedLC = signedLCertificateRepository.findByTime(time.toByteArray());
    return SignedLCs.newBuilder()
        .addAllSignedLocationCertificate(
            signedLC.stream().map(SignedLCertificate::toProtobuf).collect(Collectors.toList()))
        .build();
  }

  public void storeSignedLCs(LogContent logContent) {
    for (int i = this.lastSignedLCIndexSaved;
        i < logContent.getSignedLocationCertificatesCount();
        i++) {
      SignedLCertificate signedLC =
          SignedLCertificate.toDomain(logContent.getSignedLocationCertificates(i));
      signedLCertificateRepository.save(signedLC);
    }

    this.lastSignedLCIndexSaved = logContent.getSignedLocationCertificatesCount();
    logger.info("Last Signed LC Index Saved: " + lastSignedLCIndexSaved);
  }

  public MTHCheck getMTHStatus(SignedTreeHead STH) {
    boolean mthCheck = false;
    if (this.STH.getMth().getTreeSize() >= STH.getMth().getTreeSize()
        && this.getSTH().getMth().getTimestamp() >= STH.getMth().getTimestamp()) {
      logger.info("Received an STH that Requires an AuditProof!");
      logger.info("Received MTH: " + STH.getMth());
      logger.info("Monitor MTH: " + this.STH.getMth());
      ConsistencyProofResult consistencyProofResult =
          restTemplate.postForObject(
              this.ledgerURL + "/consistency-proof", STH, ConsistencyProofResult.class);
      if (consistencyProofResult == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Error When Requesting A Consistency Proof!");
      }

      if ((this.STH.getMth().getTreeSize() == STH.getMth().getTreeSize()
              && consistencyProofResult.getConsistencyProofHashesCount() == 0)
          || (this.STH.getMth().getTreeSize() > STH.getMth().getTreeSize()
              && consistencyProofResult.getConsistencyProofHashesCount() > 0)) {
        mthCheck = true;
      }
    }
    return MTHCheck.newBuilder().setIsMTHEqual(mthCheck).build();
  }
}
