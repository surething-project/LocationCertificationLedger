package pt.ulisboa.tecnico.transparency.ledger;

import com.google.protobuf.InvalidProtocolBufferException;
import eu.surething_project.core.Signature;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;

import javax.persistence.*;
import java.util.Arrays;

@Entity
@Table
public class STH {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Embedded private MTH mth;
  private byte[] signature;

  public STH(MTH mth, byte[] signature) {
    this.mth = mth;
    this.signature = signature;
  }

  public STH() {
  }

  public SignedTreeHead toProtobuf() {
    try {
      return SignedTreeHead.newBuilder()
              .setMth(this.mth.toProtobuf())
              .setSignature(Signature.parseFrom(this.signature))
              .build();
    } catch (InvalidProtocolBufferException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error Transforming Signature");
    }
  }

  public static STH toDomain(SignedTreeHead sth) {
    return new STH(MTH.toDomain(sth.getMth()), sth.getSignature().toByteArray());
  }

  @Override
  public String toString() {
    return "SignedTreeHead{"
        + "MTH="
        + this.mth
        + ", signature="
        + Arrays.toString(signature)
        + '}';
  }
}
