package de.kaliburg.morefair.game.round.ladder;

import de.kaliburg.morefair.api.FairController;
import de.kaliburg.morefair.dto.ChatDTO;
import de.kaliburg.morefair.game.round.RoundEntity;
import de.kaliburg.morefair.game.round.ranker.RankerEntity;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
@Table(name = "ladder", uniqueConstraints = {
    @UniqueConstraint(name = "uq_uuid", columnNames = "uuid"),
    @UniqueConstraint(name = "uk_number_round", columnNames = {"number",
        "round_id"})})
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@RequiredArgsConstructor
@SequenceGenerator(name = "seq_ladder", sequenceName = "seq_ladder", allocationSize = 1)
public final class LadderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ladder")
  private Long id;
  @NonNull
  @Column(nullable = false)
  private UUID uuid;
  @NonNull
  @Column(nullable = false)
  private Integer number;
  // TODO: @NonNull
  @ManyToOne
  @JoinColumn(name = "round_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ladder_round"))
  private RoundEntity round;
  @OneToMany(mappedBy = "ladder", fetch = FetchType.LAZY)
  private List<RankerEntity> rankers = new ArrayList<>();

  public ChatDTO convertToChatDTO() {
    return new ChatDTO(this);
  }

  public Integer getRequiredRankerCountToUnlock() {
    return Math.max(FairController.MINIMUM_PEOPLE_FOR_PROMOTE, getNumber());
  }

  public BigInteger getRequiredPointsToUnlock() {
    return FairController.POINTS_FOR_PROMOTE.multiply(BigInteger.valueOf(getNumber()));
  }
}
