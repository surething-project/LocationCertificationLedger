package pt.ulisboa.tecnico.transparency.verifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.verifier.contract.UserOuterClass.Authorization;
import pt.ulisboa.tecnico.transparency.verifier.contract.UserOuterClass.Credentials;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
  public static Logger logger = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final PBEService pbeService;

  @Autowired
  public UserService(UserRepository userRepository, PBEService pbeService) {
    this.userRepository = userRepository;
    this.pbeService = pbeService;
  }

  public Authorization register(Credentials credentials) {
    Optional<User> userDB = userRepository.findUserByUsername(credentials.getUsername());
    if (userDB.isPresent()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account Already Exists");
    }
    byte[] passwordSalt = this.pbeService.newSalt();
    try {
      byte[] passwordHash = this.pbeService.hash(credentials.getPassword(), passwordSalt);
      User user = new User(credentials.getUsername(), passwordHash, passwordSalt);
      this.userRepository.save(user);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error When Verifying User!");
    }
    return Authorization.newBuilder()
        .setJwt(JWTAuthorizationFilter.createJwt(List.of("USER")))
        .build();
  }

  public Authorization login(Credentials credentials) {
    Optional<User> userDB = userRepository.findUserByUsername(credentials.getUsername());
    if (userDB.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid User!");
    }
    User user = userDB.get();
    logger.info("User Found! " + credentials.getUsername());
    try {
      if (!pbeService.isExpectedPwd(
          credentials.getPassword(), user.getPasswordHash(), user.getPasswordSalt())) {
        logger.error("Invalid Password for User: " + credentials.getUsername());
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid User!");
      }
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      logger.error(e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error When Verifying User!");
    }
    return Authorization.newBuilder()
        .setJwt(JWTAuthorizationFilter.createJwt(List.of("ROLE_USER")))
        .build();
  }

  public Authorization refresh() {
    return Authorization.newBuilder()
        .setJwt(JWTAuthorizationFilter.createJwt(List.of("ROLE_USER")))
        .build();
  }
}
