package pt.ulisboa.tecnico.transparency.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pt.ulisboa.tecnico.transparency.RestTemplateErrorHandler;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree;
import pt.ulisboa.tecnico.transparency.contract.MerkleTree.SignedTreeHead;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.ConsistencyProofResult;
import pt.ulisboa.tecnico.transparency.ledger.contract.Ledger.LogContent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class MonitorCommandLineRunner implements CommandLineRunner {

  public static Logger logger = LoggerFactory.getLogger(MonitorCommandLineRunner.class);
  private final RestTemplate restTemplate;
  private final MonitorService monitorService;

  @Value("${ledger.mmd}")
  private int ledgerMMD;

  @Value("${url.ledger}")
  private String ledgerURL;

  @Value("${url.auditor}")
  private String auditorUrl;

  @Autowired
  public MonitorCommandLineRunner(RestTemplate restTemplate, MonitorService monitorService) {
    this.restTemplate = restTemplate;
    this.monitorService = monitorService;
  }

  public LogContent getLedgerLogContent() {
    LogContent logContent = this.restTemplate.getForObject(this.ledgerURL, LogContent.class);
    if (logContent == null) {
      logger.error("Ledger returned Invalid Log Content");
    } else {
      logger.info(String.valueOf(logContent));
      monitorService.storeSignedLCs(logContent);
    }
    return logContent;
  }

  public void gossipAuditor(LogContent logContent) {
    SignedTreeHead STH = logContent.getSTH();
    ConsistencyProofResult consistencyProofResult =
            this.restTemplate
            .postForEntity(this.auditorUrl + "/gossip", STH, ConsistencyProofResult.class)
            .getBody();
    if (consistencyProofResult == null) {
      logger.error("Auditor returned an Invalid Consistency Proof Result");
      return;
    }
    MerkleTree.MerkleTreeHead myMTH = monitorService.getSTH().getMth();
    MerkleTree.MerkleTreeHead auditorMTH = consistencyProofResult.getSTH().getMth();
    if (myMTH.getTreeSize() <= myMTH.getTreeSize()
        && myMTH.getTimestamp() <= auditorMTH.getTimestamp()) {
      logger.info("Swapping STHs with Auditor!");
      monitorService.setSTH(consistencyProofResult.getSTH());
    }
  }

  @Override
  public void run(String... args) {

    class DownloadLogContentsAndGossip extends TimerTask {
      public void run() {
        logger.info("Getting Ledger Logs Content!");
        LogContent logContent = getLedgerLogContent();
        if (logContent == null) return;
        logger.info("Doing Gossip with Auditor!");
        gossipAuditor(logContent);
      }
    }

    // And From your main() method or any other method
    Timer timer = new Timer();
    timer.schedule(new DownloadLogContentsAndGossip(), ledgerMMD, ledgerMMD / 2);
  }
}
