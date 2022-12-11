package pt.ulisboa.tecnico.transparency.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface SignedLCertificateRepository extends JpaRepository<SignedLCertificate, Long> {
}
