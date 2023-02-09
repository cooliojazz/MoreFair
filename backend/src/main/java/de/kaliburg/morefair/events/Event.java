package de.kaliburg.morefair.events;

import de.kaliburg.morefair.events.types.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class Event {

  @NonNull
  private EventType eventType;
  @NonNull
  private Long accountId;
  private Object data;
}
