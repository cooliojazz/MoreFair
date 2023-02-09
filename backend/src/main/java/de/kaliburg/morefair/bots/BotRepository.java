package de.kaliburg.morefair.bots;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotRepository extends JpaRepository<BotEntity, Long> {

}
