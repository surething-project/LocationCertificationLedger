package pt.ulisboa.tecnico.transparency.ledger;

import eu.surething_project.core.LocationCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.*;

@RestController
@RequestMapping(path = "v2/ledger")
public class LedgerController {
  public static Logger logger = LoggerFactory.getLogger(LedgerController.class);
  private final LedgerService ledgerService;

  @Autowired
  public LedgerController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @PostMapping
  public SLCT storeNewLP(@RequestBody LocationCertificate locationCertificate) {
    return ledgerService.storeNewSignedLC(locationCertificate);
  }

  @GetMapping
  public LogContent getSignedLCs() {
    return ledgerService.getSignedLCs();
  }

  @RequestMapping(value = "/audit-proof", method = RequestMethod.POST)
  public AuditResult getAuditProof(@RequestBody AuditRequest auditRequest) {
      return ledgerService.auditProof(auditRequest);
  }

  @RequestMapping(value = "/consistency-proof", method = RequestMethod.POST)
  public ConsistencyProofResult getConsistencyProof(
      @RequestBody ConsistencyProofRequest consistencyProofRequest) {
    return ledgerService.getConsistencyProof(consistencyProofRequest);
  }

  @RequestMapping(value = "/certificate", method = RequestMethod.GET)
  public Certificate getCertificate() {
    return ledgerService.getCertificate();
  }
}
