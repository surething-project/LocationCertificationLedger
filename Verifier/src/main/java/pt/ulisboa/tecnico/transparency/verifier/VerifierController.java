package pt.ulisboa.tecnico.transparency.verifier;

import eu.surething_project.core.SignedLocationClaim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SLCT;

@RestController
@RequestMapping(path = "v2/verifier")
public class VerifierController {
  private final VerifierService verifierService;

  @Autowired
  public VerifierController(VerifierService verifierService) {
    this.verifierService = verifierService;
  }

  @PostMapping
  public SLCT storeNewSignedLC(@RequestBody SignedLocationClaim signedLClaim) {
    return verifierService.storeNewSignedLC(signedLClaim);
  }
}
