package pt.ulisboa.tecnico.transparency.auditor;

import eu.surething_project.signature.util.SignatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.MerkleTreeHead;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.*;
import pt.ulisboa.tecnico.transparency.monitor.contract.Monitor.MTHCheck;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditorService {
  public static Logger logger = LoggerFactory.getLogger(AuditorService.class);
  private final RestTemplate restTemplate;
  private SignedTreeHead STH = SignedTreeHead.newBuilder().build();

  @Value("${ledger.url}")
  private String ledgerURL;

  @Autowired
  public AuditorService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public SignedTreeHead getSTH() {
    return STH;
  }

  public void setSTH(SignedTreeHead STH) {
    this.STH = STH;
  }

  public byte[] getHash(String input) throws NoSuchAlgorithmException {
    return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
  }

  public String toHexString(byte[] hash) {
    String hex = new BigInteger(1, hash).toString(16);
    StringBuilder hexString = new StringBuilder(hex);

    // Pad with leading zeros
    while (hexString.length() < 32) hexString.insert(0, '0');
    return hexString.toString();
  }

  public String merkleTreeHead(List<SignedLocationCertificate> entries) {
    String originalString = "";
    String result = "";
    try {
      if (entries.size() == 0) {
        result = toHexString(getHash(originalString));
      } else if (entries.size() == 1) {
        byte[] bytes0 = new byte[] {(byte) 0x00};
        String string0 = new String(bytes0, StandardCharsets.UTF_8);
        originalString = string0 + entries.get(0);
        result = toHexString(getHash(originalString));
      } else {
        int k = 1;
        while (k < entries.size()) {
          k = 2 * k;
        }
        k = k / 2;
        byte[] bytes1 = new byte[] {(byte) 0x01};
        String string1 = new String(bytes1, StandardCharsets.UTF_8);
        originalString =
            string1
                + merkleTreeHead(entries.subList(0, k))
                + merkleTreeHead(entries.subList(k, entries.size()));
        result = toHexString(getHash(originalString));
      }
    } catch (NoSuchAlgorithmException e) {
      logger.error("Exception thrown for incorrect algorithm: " + e);
    }
    return result;
  }

  public List<String> path(Long position, List<SignedLocationCertificate> entries) {
    // position refers to the position in tree starting at 0
    List<String> returnList = new ArrayList<>();
    List<String> temporaryList = new ArrayList<>();
    if (entries.size() <= 1) return returnList;

    int k = 1;
    while (k < entries.size()) k = 2 * k;
    k = k / 2;
    if (position < k) {
      temporaryList.add(merkleTreeHead(entries.subList(k, entries.size())));
      returnList.addAll(path(position, entries.subList(0, k)));
      returnList.addAll(temporaryList);
    } else {
      temporaryList.add(merkleTreeHead(entries.subList(0, k)));
      returnList.addAll(path(position - k, entries.subList(k, entries.size())));
      returnList.addAll(temporaryList);
    }
    return returnList;
  }

  public boolean verifyAuditPath(List<String> auditPath, List<String> auditPathCalculated) {
    for (int i = 0; i < auditPathCalculated.size(); i++) {
      if (!auditPath.get(i).equals(auditPathCalculated.get(i))) return false;
    }
    return true;
  }

  public AuditResult getAuditProof(SLCT slct) {
    logger.info("Getting an Audit Proof");
    logger.info("Tree size = " + this.STH.getMth().getTreeSize());

    AuditRequest auditRequest =
        AuditRequest.newBuilder()
            .setSlct(slct)
            .setTreeSize(this.STH.getMth().getTreeSize())
            .build();

    AuditResult auditResult =
        this.restTemplate.postForObject(
            ledgerURL + "/audit-proof", auditRequest, AuditResult.class);
    if (auditResult == null) {
      logger.error("Ledger Returned Null!");
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Currently the Ledger says it has no logs! ");
    }
    logger.info("Received an AuditResult From Ledger!");

    SignedTreeHead sth = auditResult.getSTH();
    MerkleTreeHead mth = sth.getMth();
    System.out.println(sth.getMth());
    try {
      FileInputStream inputStream = new FileInputStream("./src/main/resources/Ledger.crt");
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      PublicKey pk = certificateFactory.generateCertificate(inputStream).getPublicKey();
      if (!SignatureManager.verify(sth.getSignature().getValue().toByteArray(), mth.toByteArray(), pk)) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Ledger Lied! Signature of MTH did not match STH.");
      }

    } catch (Exception e) {
      logger.error(e.getMessage());
      return null;
    }

    List<String> auditPath = new ArrayList<>(auditResult.getAuditPathList());

    Long leafIndex = auditResult.getLeafIndex();
    List<SignedLocationCertificate> slcListLedger =
        auditResult.getSignedLocationCertificatesList();

    SignedLocationCertificate slcProver = slct.getSignedLocationCertificate();
    SignedLocationCertificate slcLedger = slcListLedger.get(leafIndex.intValue());

    boolean equalIDs = slcProver.getId().equals(slcLedger.getId());
    if (!equalIDs) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Ledger Lied! SLC IDs from Prover differs from SLC IDs From Ledger");
    }
    boolean equalLCerts =
        slcProver.getLocationCertificate().equals(slcLedger.getLocationCertificate());
    if (!equalLCerts) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Ledger Lied! SLC Certificate from Prover differs from SLC Certificate From Ledger");
    }
    boolean equalSignatures = slcProver.getSignature().equals(slcLedger.getSignature());
    if (!equalSignatures) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Ledger Lied! SLC Signature from Prover differs from SLC Signature From Ledger");
    }

    List<String> auditPathCalculated = path(leafIndex, slcListLedger);
    if (auditPath.size() != auditPathCalculated.size()
        || !verifyAuditPath(auditPath, auditPathCalculated)) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Ledger Lied! AuditPath Calculated differs from the received from ledger");
    }

    logger.info("AuditPaths Are The Same!");
    MTHCheck mthCheck =
        this.restTemplate.postForObject(this.ledgerURL + "/mth-check", sth, MTHCheck.class);
    if (mthCheck != null) logger.info("MTHCheck = " + mthCheck.getIsMTHEqual());
    return auditResult;
  }

  public ConsistencyProofResult gossipSTH(SignedTreeHead STH) {
    if (!(this.getSTH().getMth().getTreeSize() <= STH.getMth().getTreeSize()
        || this.getSTH().getMth().getTimestamp() <= STH.getMth().getTimestamp()))
      return ConsistencyProofResult.newBuilder().build();

    logger.info("Going to ask for consistency proof");
    logger.info("MTH1: " + this.STH.getMth());
    logger.info("MTH2: " + STH.getMth());

    ConsistencyProofRequest consistencyProofRequest =
        ConsistencyProofRequest.newBuilder().setSth1(this.STH).setSth2(STH).build();

    ConsistencyProofResult consistencyProofResult =
        this.restTemplate.postForObject(
            this.ledgerURL + "/consistency-proof",
            consistencyProofRequest,
            ConsistencyProofResult.class);

    if (consistencyProofResult == null) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Ledger Lied! ConsistencyProofResult");
    }
    logger.info(
        "Consistency Proof Hashes: " + consistencyProofResult.getConsistencyProofHashesList());

    List<String> proofHashes = consistencyProofResult.getConsistencyProofHashesList();
    long myTreeSize = this.getSTH().getMth().getTreeSize();
    long treeSize = STH.getMth().getTreeSize();
    if ((myTreeSize == treeSize && proofHashes.size() == 0)
        || (myTreeSize < treeSize && proofHashes.size() > 0)) {
      logger.info("Updating my STH!");
      this.setSTH(STH);
    } else {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Ledger is Lying! Impossible Consistency Proof");
    }
    return consistencyProofResult;
  }
}
