package pt.ulisboa.tecnico.transparency.ledger;

import eu.surething_project.core.LocationCertificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.*;

@RestController
@RequestMapping(path = "v2/ledger")
public class LedgerController {

  private final LedgerService ledgerService;

  @Autowired
  public LedgerController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @PostMapping
  public SLCT storeNewSignedLC(@RequestBody LocationCertificate locationCertificate) {
    return ledgerService.storeNewSignedLC(locationCertificate);
  }

  @GetMapping
  public LogContent getSignedLCs() {
    return ledgerService.getSignedLCs();
  }

  @RequestMapping(value = "/audit-proof", method = RequestMethod.POST)
  public AuditResult getAuditProof(@RequestBody AuditRequest auditRequest) {
    Long treeSize = auditRequest.getTreeSize();
    SLCT slct = auditRequest.getSlct();
    SignedLocationCertificate signedLCert = slct.getSignedLocationCertificate();
    try {
      String lpHash =
          ledgerService.toHexString(
              ledgerService.getHash(signedLCert.getLocationCertificate().toString()));
      return ledgerService.maliciousAuditProof(treeSize, lpHash);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @RequestMapping(value = "/consistency-proof", method = RequestMethod.POST)
  public ConsistencyProofResult getConsistencyProof(
      @RequestBody ConsistencyProofRequest consistencyProofRequest) throws Exception {
    return ledgerService.getConsistencyProof(consistencyProofRequest);
  }

  @RequestMapping(value = "/certificate", method = RequestMethod.GET)
  public Certificate getCertificate() {
    return ledgerService.getCertificate();
  }
}
