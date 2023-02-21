package de.kaliburg.morefair.bots;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.kaliburg.morefair.account.AccountEntity;
import io.hypersistence.utils.hibernate.type.array.DoubleArrayType;
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
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "bot", uniqueConstraints = @UniqueConstraint(name = "uk_uuid", columnNames = "uuid"))
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@SequenceGenerator(name = "seq_bot", sequenceName = "seq_bot", allocationSize = 1)
@TypeDef(name = "double[]", typeClass = DoubleArrayType.class)
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
  
  @Type(type = "double[]")
  @Column(columnDefinition = "double precision[]")
  private double[] schedule = {};
  
  private int currentSchedule = 0;
  
  private boolean awake = true;
  
  @Transient
  private boolean loggedIn = false;
  
  @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonBinaryType")
  @Column(columnDefinition = "jsonb")
  private ObjectNode data = JsonNodeFactory.instance.objectNode();
  
  @NonNull
  @Column(nullable = false)
  private String name;

  public BotEntity(UUID uuid, BotType type, String name, double[] schedule) {
    this.uuid = uuid;
	this.type = type;
	this.name = name;
	this.schedule = schedule;
  }
  
  public String getDisplayName() {
	return name + " " + type.getIcon() + "🤖" + (awake ? "" : " 💤");
  }
  
  public void updateSchedule(double schedulePercent) {
	if (schedulePercent > schedule[currentSchedule]) {
      currentSchedule++;
//	  if (currentSchedule >= schedule.length) currentSchedule = 0;
	  awake = !awake;
	}
	if (currentSchedule > 1 && schedulePercent < schedule[currentSchedule - 1]) {
		currentSchedule = 0;
	}
  }
}
