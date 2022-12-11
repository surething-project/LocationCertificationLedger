package pt.ulisboa.tecnico.transparency.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface STHRepository extends JpaRepository<STH, Long> {
}
