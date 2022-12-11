package pt.ulisboa.tecnico.transparency.auditor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.AuditResult;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.ConsistencyProofResult;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.SLCT;

@RestController
@RequestMapping(path = "v2/auditor")
public class AuditorController {
  private final AuditorService auditorService;

  @Autowired
  public AuditorController(AuditorService auditorService) {
    this.auditorService = auditorService;
  }

  @PostMapping
  public AuditResult getAuditProof(@RequestBody SLCT slct) {
    return auditorService.getAuditProof(slct);
  }

  @RequestMapping(value = "/gossip", method = RequestMethod.POST)
  public ConsistencyProofResult gossipSTH(@RequestBody SignedTreeHead STH) {
    return auditorService.gossipSTH(STH);
  }
}
