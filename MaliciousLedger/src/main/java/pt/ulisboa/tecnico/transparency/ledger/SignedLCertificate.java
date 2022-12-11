package pt.ulisboa.tecnico.transparency.ledger;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import eu.surething_project.core.Signature;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SignedLocationCertificate;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

@Entity
@Table
public class SignedLCertificate {
  @Id
  private String id;

  @Embedded private LCertificate locationCertificate;

  @OneToOne(fetch = FetchType.EAGER)
  private STH sth;

  private byte[] ledgerSignature;

  public SignedLCertificate(LCertificate locationCertificate, byte[] ledgerSignature) {
    this.id = UUID.randomUUID().toString();
    this.locationCertificate = locationCertificate;
    this.ledgerSignature = ledgerSignature;
  }

  public SignedLCertificate() {}

  public static SignedLCertificate toDomain(SignedLocationCertificate signedLocationCertificate) {
    LCertificate lCertificate =
        LCertificate.toDomain(signedLocationCertificate.getLocationCertificate());
    return new SignedLCertificate(
        lCertificate, signedLocationCertificate.getSignature().toByteArray());
  }

  public LCertificate getLocationCertificate() {
    return locationCertificate;
  }

  public STH getSth() {
    return sth;
  }

  public void setSth(STH sth) {
    this.sth = sth;
  }

  public SignedLocationCertificate toProtobuf() {
    Random randomLCert = new Random();
    Signature signature =
        Signature.newBuilder()
            .setCryptoAlgo("SHA256withRSA")
            .setValue(ByteString.copyFrom(ledgerSignature))
            .setNonce(randomLCert.nextLong())
            .build();
    try {
      return SignedLocationCertificate.newBuilder()
          .setId(this.id)
          .setLocationCertificate(this.locationCertificate.toProtobuf())
          .setSignature(signature)
          .build();

    } catch (InvalidProtocolBufferException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Error While Transforming SignedLocationCertificate to Proto!");
    }
  }

  @Override
  public String toString() {
    return "SignedLCertificate{"
        + "id="
        + id
        + ", locationCertificate="
        + locationCertificate
        + ", ledgerSignature="
        + Arrays.toString(ledgerSignature)
        + '}';
  }
}
