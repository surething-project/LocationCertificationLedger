package pt.ulisboa.tecnico.transparency.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

@Component
public class LedgerCommandLineRunner implements CommandLineRunner {

  public static Logger logger = LoggerFactory.getLogger(LedgerCommandLineRunner.class);
  private final LedgerService ledgerService;

  @Value("${ledger.mmd}")
  private int ledgerMMD;

  @Autowired
  public LedgerCommandLineRunner(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @Override
  public void run(String... args) {
    class StoreNewLPS extends TimerTask {
      public void run() {
        logger.info("Persisting If new Signed LCs Appeared!");
        ledgerService.storeNewSignedLCs();
      }
    }
    Timer timerStoreNewLPS = new Timer();
    timerStoreNewLPS.schedule(new StoreNewLPS(), ledgerMMD / 2, ledgerMMD / 2);
  }
}
