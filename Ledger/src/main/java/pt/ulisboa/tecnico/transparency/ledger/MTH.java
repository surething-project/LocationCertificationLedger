package pt.ulisboa.tecnico.transparency.ledger;

import pt.ulisboa.tecnico.transparency.contract.MerkleTree.MerkleTreeHead;

import javax.persistence.Embeddable;

@Embeddable
public class MTH {

  private Long timestamp;
  private Long treeSize;
  private String merkleTreeRoot;

  public MTH(Long timestamp, Long treeSize, String merkleTreeRoot) {
    this.timestamp = timestamp;
    this.treeSize = treeSize;
    this.merkleTreeRoot = merkleTreeRoot;
  }

  public MTH() {}

  public static MTH toDomain(MerkleTreeHead mth) {
    return new MTH(mth.getTimestamp(), mth.getTreeSize(), mth.getMerkleTreeRoot());
  }

  public MerkleTreeHead toProtobuf() {
    return MerkleTreeHead.newBuilder()
        .setTimestamp(this.timestamp)
        .setTreeSize(this.treeSize)
        .setTimestamp(this.timestamp)
        .build();
  }

  @Override
  public String toString() {
    return "MerkleTreeHead{"
        + "timestamp="
        + timestamp
        + ", treeSize="
        + treeSize
        + ", merkleTreeRoot='"
        + merkleTreeRoot
        + '\''
        + '}';
  }
}
