package pt.ulisboa.tecnico.transparency.monitor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignedLCertificateRepository extends JpaRepository<SignedLCertificate, Long> {

  @Query(
      "select sl from SignedLCertificate sl where sl.locationCertificate.verifierId = :verifierId")
  List<SignedLCertificate> findByVerifierId(@Param("verifierId") String verifierId);

  @Query(
      "select sl from SignedLCertificate sl where sl.locationCertificate.evidenceType = :evidenceType")
  List<SignedLCertificate> findByEvidenceType(@Param("evidenceType") String evidenceType);

  @Query("select sl from SignedLCertificate sl where sl.locationCertificate.time = :time")
  List<SignedLCertificate> findByTime(@Param("time") byte[] time);
}
