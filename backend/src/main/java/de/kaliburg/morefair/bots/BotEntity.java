package de.kaliburg.morefair.bots;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.kaliburg.morefair.account.AccountEntity;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "bot", uniqueConstraints = @UniqueConstraint(name = "uk_uuid", columnNames = "uuid"))
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@SequenceGenerator(name = "seq_bot", sequenceName = "seq_bot", allocationSize = 1)
public class BotEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_bot")
  private Long id;
  
  @ManyToOne
  @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "fk_ranker_account"))
  private AccountEntity account;
  
  @NonNull
  @Column(nullable = false)
  private UUID uuid;
  
  private int currentLadder;
  
  private BotType type;
  
  @Transient
  private boolean loggedIn = false;
  
  @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonBinaryType")
  @Column(columnDefinition = "jsonb")
  private ObjectNode data = JsonNodeFactory.instance.objectNode();
  
  @NonNull
  @Column(nullable = false)
  private String name;

  public BotEntity(UUID uuid, BotType type, String name) {
    this.uuid = uuid;
	this.type = type;
	this.name = name;
  }
  
}
