package de.kaliburg.morefair.game.round;

import de.kaliburg.morefair.account.AccountEntity;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Table(name = "ranker", uniqueConstraints = @UniqueConstraint(name = "uk_uuid", columnNames = "uuid"))
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@RequiredArgsConstructor
@SequenceGenerator(name = "seq_ranker", sequenceName = "seq_ranker", allocationSize = 1)
public class RankerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ranker")
  private Long id;
  @NonNull
  @Column(nullable = false)
  private UUID uuid = UUID.randomUUID();
  @NonNull
  @Column(nullable = false, precision = 1000)
  private BigInteger points = BigInteger.ZERO;
  @NonNull
  @Column(nullable = false, precision = 1000)
  private BigInteger power = BigInteger.ONE;
  @NonNull
  @Column(nullable = false)
  private Integer bias = 0;
  @NonNull
  @Column(nullable = false)
  private Integer multiplier = 1;
  @Column
  private boolean growing = true;
  @NonNull
  @ManyToOne
  @JoinColumn(name = "ladder_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ranker_ladder"))
  private LadderEntity ladder;
  @NonNull
  @ManyToOne
  @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ranker_account"))
  private AccountEntity account;
  @NonNull
  @Column(nullable = false)
  private Integer rank;
  @Column(nullable = false, precision = 1000, scale = 0)
  private BigInteger grapes = BigInteger.ZERO;
  @NonNull
  @Column(nullable = false, precision = 1000, scale = 0)
  private BigInteger vinegar = BigInteger.ZERO;
  @Column
  private boolean autoPromote = false;
  @NonNull
  @OneToOne(mappedBy = "ranker", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private UnlocksEntity unlocks = new UnlocksEntity(this);


  public RankerEntity addPoints(Integer points, double secondsPassed) {
    return addPoints(BigInteger.valueOf(points), secondsPassed);
  }

  public RankerEntity addPoints(BigInteger points, double secondsPassed) {
	this.points = this.points.add(new BigDecimal(points)
	    .multiply(BigDecimal.valueOf(secondsPassed)).toBigInteger());
    return this;
  }

  public RankerEntity addPower(BigInteger power, double secondsPassed) {
    BigDecimal decPower = new BigDecimal(power);
    decPower = decPower.multiply(BigDecimal.valueOf(secondsPassed));
    this.power = this.power.add(decPower.toBigInteger());
    return this;
  }

  public RankerEntity addPower(Integer power, double secondsPassed) {
    return addPower(BigInteger.valueOf(power), secondsPassed);
  }

  public RankerEntity addVinegar(Integer vinegar, double secondsPassed) {
    return addVinegar(BigInteger.valueOf(vinegar), secondsPassed);
  }

  public RankerEntity addVinegar(BigInteger vinegar, double secondsPassed) {
    BigDecimal decVinegar = new BigDecimal(vinegar);
    decVinegar = decVinegar.multiply(BigDecimal.valueOf(secondsPassed));
    this.vinegar = this.vinegar.add(decVinegar.toBigInteger());
    return this;
  }

  public RankerEntity mulVinegar(double multiplier, double deltaSec) {
    BigDecimal decVinegar = new BigDecimal(this.vinegar);
    this.vinegar = decVinegar.multiply(BigDecimal.valueOf(Math.pow(multiplier, deltaSec)))
        .toBigInteger();
    return this;
  }

  public RankerEntity addGrapes(Integer grapes, double secondsPassed) {
    return addGrapes(BigInteger.valueOf(grapes), secondsPassed);
  }

  public RankerEntity addGrapes(BigInteger grapes, double secondsPassed) {
    BigDecimal decGrapes = new BigDecimal(grapes);
    decGrapes = decGrapes.multiply(BigDecimal.valueOf(secondsPassed));
    this.grapes = this.grapes.add(decGrapes.toBigInteger());
    return this;
  }

}
