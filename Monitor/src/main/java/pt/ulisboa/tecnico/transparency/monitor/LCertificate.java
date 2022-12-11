package pt.ulisboa.tecnico.transparency.monitor;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import eu.surething_project.core.LocationCertificate;
import eu.surething_project.core.LocationVerification;
import eu.surething_project.core.Signature;
import eu.surething_project.core.Time;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Embeddable
public class LCertificate {
  private String verifierId;
  private String claimId;
  private byte[] time;
  private String evidenceType;
  private byte[] evidence;
  private byte[] verifierSignature;
  @ElementCollection private List<String> endorsementIds;

  public LCertificate() {}

  public LCertificate(
      String verifierId,
      String claimId,
      ProtocolStringList endorsementIds,
      byte[] time,
      String evidenceType,
      byte[] evidence,
      byte[] verifierSignature) {
    this.verifierId = verifierId;
    this.claimId = claimId;
    this.endorsementIds = new ArrayList<>(endorsementIds);
    this.time = time;
    this.evidenceType = evidenceType;
    this.evidence = evidence;
    this.verifierSignature = verifierSignature;
  }

  public static LCertificate toDomain(LocationCertificate locationCertificate) {
    return new LCertificate(
        locationCertificate.getVerification().getVerifierId(),
        locationCertificate.getVerification().getClaimId(),
        locationCertificate.getVerification().getEndorsementIdsList(),
        locationCertificate.getVerification().getTime().toByteArray(),
        locationCertificate.getVerification().getEvidenceType(),
        locationCertificate.getVerification().getEvidence().toByteArray(),
        locationCertificate.getVerifierSignature().toByteArray());
  }

  public LocationCertificate toProtobuf() throws InvalidProtocolBufferException {
    LocationVerification locationVerification =
        LocationVerification.newBuilder()
            .setVerifierId(this.verifierId)
            .setClaimId(this.claimId)
            .addAllEndorsementIds(this.endorsementIds)
            .setTime(Time.parseFrom(time))
            .setEvidenceType(this.evidenceType)
            .setEvidence(Any.parseFrom(this.evidence))
            .build();

    return LocationCertificate.newBuilder()
        .setVerification(locationVerification)
        .setVerifierSignature(Signature.parseFrom(this.verifierSignature))
        .build();
  }

  @Override
  public String toString() {
    return "LedgerLocationCertificate{"
        + "verifierId='"
        + verifierId
        + '\''
        + ", claimId='"
        + claimId
        + '\''
        + ", endorsementIds="
        + endorsementIds
        + ", time="
        + Arrays.toString(time)
        + ", evidenceType='"
        + evidenceType
        + '\''
        + ", evidence="
        + Arrays.toString(evidence)
        + ", verifierSignature="
        + Arrays.toString(verifierSignature)
        + '}';
  }
}
