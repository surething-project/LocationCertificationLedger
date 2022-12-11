package pt.ulisboa.tecnico.transparency.ledger;

import com.google.protobuf.ByteString;
import eu.surething_project.core.LocationCertificate;
import eu.surething_project.core.Signature;
import eu.surething_project.signature.util.SignatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.MerkleTreeHead;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.Certificate;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class LedgerService {
  public static Logger logger = LoggerFactory.getLogger(LedgerCommandLineRunner.class);
  private final SignedLCertificateRepository signedLCertificateRepository;
  private final STHRepository sthRepository;
  private final List<SignedLCertificate> signedLCs;
  private final PrivateKey privateKey;
  private int lastSignedLCIndexSaved;

  @Value("${ledger.id}")
  private int ledgerId;

  @Autowired
  public LedgerService(
      SignedLCertificateRepository signedLCertificateRepository, STHRepository sthRepository) {
    try {
      FileInputStream is = new FileInputStream("src/main/resources/Ledger.p12");
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(is, "Ledger".toCharArray());
      this.privateKey = (PrivateKey) keystore.getKey("LedgerCert", "Ledger".toCharArray());
    } catch (IOException | GeneralSecurityException e) {
      logger.error("Error While Trying to Get Private Key of Ledger: " + e.getMessage());
      throw new RuntimeException(e);
    }
    this.signedLCertificateRepository = signedLCertificateRepository;
    this.sthRepository = sthRepository;
    this.signedLCs = this.signedLCertificateRepository.findAll();
    this.lastSignedLCIndexSaved = this.signedLCs.size();
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

  public String merkleTreeHead(List<SignedLCertificate> entries) {
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

  public List<String> path(Long position, List<SignedLCertificate> entries) {
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

  public List<String> proof(int numberOfKnownElements, List<SignedLCertificate> newTree) {
    return subProof(numberOfKnownElements, newTree, true);
  }

  public List<String> subProof(
      int numberOfKnownElements, List<SignedLCertificate> newTree, boolean bool) {
    List<String> returnList = new ArrayList<>();
    if (numberOfKnownElements == newTree.size() && bool) {
      return returnList;
    } else if (numberOfKnownElements == newTree.size()) {
      returnList.add(merkleTreeHead(newTree));
      return returnList;
    } else {
      List<String> temporaryList = new ArrayList<>();
      int k = 1;
      while (k < newTree.size()) {
        k = 2 * k;
      }
      k = k / 2;
      if (numberOfKnownElements <= k) {
        temporaryList.add(merkleTreeHead(newTree.subList(k, newTree.size())));
        returnList.addAll(subProof(numberOfKnownElements, newTree.subList(0, k), bool));
        returnList.addAll(temporaryList);
      } else {
        temporaryList.add(merkleTreeHead(newTree.subList(0, k)));
        returnList.addAll(
            subProof(numberOfKnownElements - k, newTree.subList(k, newTree.size()), false));
        returnList.addAll(temporaryList);
      }
    }
    return returnList;
  }

  public synchronized SLCT storeNewSignedLC(LocationCertificate locationCertificate) {
    logger.info("Received LC!");
    Random randomLCert = new Random();
    SignedLocationCertificate signedLCertificate =
        generateSignedLCertificate(locationCertificate, randomLCert);
    this.signedLCs.add(SignedLCertificate.toDomain(signedLCertificate));
    return SLCT.newBuilder()
        .setLogId(ledgerId)
        .setTimestamp(System.currentTimeMillis())
        .setSignedLocationCertificate(signedLCertificate)
        .build();
  }

  private SignedLocationCertificate generateSignedLCertificate(
      LocationCertificate locationCertificate, Random randomLCert) {
    return SignedLocationCertificate.newBuilder()
        .setLocationCertificate(locationCertificate)
        .setSignature(
            Signature.newBuilder()
                .setCryptoAlgo("SHA256withRSA")
                .setValue(ByteString.copyFrom(signLocationCertificate(locationCertificate)))
                .setNonce(randomLCert.nextLong())
                .build())
        .build();
  }

  private synchronized SignedTreeHead generateSTH() {
    MerkleTreeHead MTH =
        MerkleTreeHead.newBuilder()
            .setTimestamp(System.currentTimeMillis())
            .setTreeSize(this.signedLCs.size())
            .setMerkleTreeRoot(merkleTreeHead(this.signedLCs))
            .build();

    Random random = new Random();
    return SignedTreeHead.newBuilder()
        .setMth(MTH)
        .setSignature(
            Signature.newBuilder()
                .setCryptoAlgo("SHA256withRSA")
                .setValue(ByteString.copyFrom(signMTH(MTH)))
                .setNonce(random.nextLong())
                .build())
        .build();
  }

  public byte[] signMTH(MerkleTreeHead MTH) {
    try {
      return SignatureManager.sign(MTH.toByteArray(), this.privateKey);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error While Signing MTH!");
    }
  }

  public byte[] signLocationCertificate(LocationCertificate locationCertificate) {
    try {
      return SignatureManager.sign(locationCertificate.toByteArray(), privateKey);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error While Signing Location Certificate");
    }
  }

  public LogContent getSignedLCs() {
    List<SignedLCertificate> signedLCertificates = signedLCertificateRepository.findAll();
    if (signedLCertificates.size() < 1) return LogContent.newBuilder().build();
    STH sth = signedLCertificates.get(signedLCertificates.size() - 1).getSth();
    return LogContent.newBuilder()
        .setId(this.ledgerId)
        .addAllSignedLocationCertificates(
            signedLCertificates.stream()
                .map(SignedLCertificate::toProtobuf)
                .collect(Collectors.toList()))
        .setSTH(sth.toProtobuf())
        .build();
  }

  public AuditResult auditProof(AuditRequest auditRequest) {
    long treeSize = auditRequest.getTreeSize();
    List<SignedLCertificate> signedLCs =
        this.signedLCertificateRepository.findAll().subList(0, Math.toIntExact(treeSize));
    long leafIndex = -1;
    List<String> auditPath = new ArrayList<>();
    try {
      for (int i = 0; i < signedLCs.size(); i++) {
        String receivedHash =
            toHexString(
                getHash(
                    SignedLCertificate.toDomain(
                            auditRequest.getSlct().getSignedLocationCertificate())
                        .toString()));
        String hash = toHexString(getHash(signedLCs.get(i).getLocationCertificate().toString()));
        if (receivedHash.equals(hash)) {
          leafIndex = i;
          auditPath = path(leafIndex, signedLCs);
        }
      }
      if (signedLCs.size() < 1) {
        return AuditResult.newBuilder().build();
      }
      STH sth = signedLCs.get(signedLCs.size() - 1).getSth();
      return AuditResult.newBuilder()
          .addAllSignedLocationCertificates(
              signedLCs.stream().map(SignedLCertificate::toProtobuf).collect(Collectors.toList()))
          .setLeafIndex(leafIndex)
          .addAllAuditPath(auditPath)
          .setSTH(sth.toProtobuf())
          .build();

    } catch (Exception e) {
      logger.info(e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error While Doing an Audit Proof!");
    }
  }

  public ConsistencyProofResult getConsistencyProof(
      ConsistencyProofRequest consistencyProofRequest) {
    logger.info("Getting a Consistency Proof");
    List<SignedLCertificate> signedLCs = this.signedLCertificateRepository.findAll();
    List<String> consistencyProofNodes;
    SignedTreeHead sth1 = consistencyProofRequest.getSth1();
    SignedTreeHead sth2 = consistencyProofRequest.getSth2();

    long treeSize1 = sth1.getMth().getTreeSize();
    long treeSize2 = sth2.getMth().getTreeSize();
    logger.info("TreeSize1: " + treeSize1);
    logger.info("TreeSize2: " + treeSize2);
    logger.info("Signed LC Size:" + signedLCs.size());

    if (treeSize1 > signedLCs.size() || treeSize2 > signedLCs.size()) {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Impossible Tree Sizes!");
    }

    if (treeSize1 < treeSize2) {
      signedLCs = signedLCs.subList(0, (int) treeSize2);
      consistencyProofNodes = proof((int) treeSize1, signedLCs);
    } else {
      signedLCs = signedLCs.subList(0, (int) treeSize1);
      consistencyProofNodes = proof((int) treeSize2, signedLCs);
    }
    if (signedLCs.size() < 1) return ConsistencyProofResult.newBuilder().build();
    STH sth = signedLCs.get(signedLCs.size() - 1).getSth();
    ConsistencyProofResult consistencyProofResult =
        ConsistencyProofResult.newBuilder()
            .addAllConsistencyProofHashes(consistencyProofNodes)
            .addAllMerkleTreeElements(
                signedLCs.stream().map(SignedLCertificate::toProtobuf).collect(Collectors.toList()))
            .setSTH(sth.toProtobuf())
            .build();

    logger.info("Returning a Consistency Proof Result!");
    return consistencyProofResult;
  }

  public synchronized void storeNewSignedLCs() {
    for (int i = this.lastSignedLCIndexSaved; i < this.signedLCs.size(); i++) {
      logger.info("Persisting a Signed LC");
      SignedLCertificate signedLCertificateDomain = this.signedLCs.get(i);
      signedLCertificateDomain.setSth(STH.toDomain(generateSTH()));
      this.sthRepository.save(signedLCertificateDomain.getSth());
      this.signedLCertificateRepository.save(signedLCertificateDomain);
    }
    this.lastSignedLCIndexSaved = this.signedLCs.size();
  }

  public Certificate getCertificate() {
    try {
      Path path = Paths.get("./src/main/resources/Ledger.crt");
      byte[] data = Files.readAllBytes(path);
      return Certificate.newBuilder().setCertificate(ByteString.copyFrom(data)).build();
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error Getting Ledger Certificate");
    }
  }
}
