package pt.ulisboa.tecnico.transparency.verifier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(unique = true)
  private String username;

  private byte[] passwordHash;
  private byte[] passwordSalt;

  public User(String username, byte[] passwordHash, byte[] passwordSalt) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.passwordSalt = passwordSalt;
  }

  public User() {}

  public String getUsername() {
    return username;
  }

  public byte[] getPasswordHash() {
    return passwordHash;
  }

  public byte[] getPasswordSalt() {
    return passwordSalt;
  }
}
