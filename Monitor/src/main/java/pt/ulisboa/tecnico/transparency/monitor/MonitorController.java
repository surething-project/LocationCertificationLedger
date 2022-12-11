package pt.ulisboa.tecnico.transparency.monitor;

import eu.surething_project.core.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;
import pt.ulisboa.tecnico.transparency.monitor.contract.Monitor;
import pt.ulisboa.tecnico.transparency.monitor.contract.Monitor.SignedLCs;

@RestController
@RequestMapping(path = "v2/monitor")
public class MonitorController {
  private final MonitorService monitorService;

  @Autowired
  public MonitorController(MonitorService monitorService) {
    this.monitorService = monitorService;
  }

  @RequestMapping(value = "/verifierId", method = RequestMethod.GET)
  public SignedLCs getLCsByVerifierId(@RequestParam String verifierId) {
    return monitorService.getLCsByVerifierId(verifierId);
  }

  @RequestMapping(value = "/evidenceType", method = RequestMethod.GET)
  public SignedLCs getLCsByEvidenceType(@RequestParam String evidenceType) {
    return monitorService.getLCsByEvidenceType(evidenceType);
  }

  @RequestMapping(value = "/time", method = RequestMethod.POST)
  public SignedLCs getLCsByTime(@RequestBody Time time) {
    return monitorService.getLCsByTime(time);
  }

  @RequestMapping(value = "/mth-check", method = RequestMethod.POST)
  public Monitor.MTHCheck getMTHStatus(@RequestBody SignedTreeHead STH) {
    return monitorService.getMTHStatus(STH);
  }
}
