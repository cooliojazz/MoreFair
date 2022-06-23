package de.kaliburg.morefair.game.chat;

import de.kaliburg.morefair.account.AccountEntity;
import de.kaliburg.morefair.api.ChatController;
import de.kaliburg.morefair.api.utils.WsUtils;
import de.kaliburg.morefair.game.chat.message.MessageDTO;
import de.kaliburg.morefair.game.chat.message.MessageEntity;
import de.kaliburg.morefair.game.chat.message.MessageService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ChatService that setups and manages the GameEntity. Also routes all the requests for the
 * Chats themselves.
 */
@Service
@Log4j2
public class ChatService {

  private final ChatRepository chatRepository;
  private final MessageService messageService;
  private final WsUtils wsUtils;
  private Map<Long, ChatEntity> currentChatMap = new HashMap<>();

  public ChatService(ChatRepository chatRepository, MessageService messageService,
      WsUtils wsUtils) {
    this.chatRepository = chatRepository;
    this.messageService = messageService;
    this.wsUtils = wsUtils;
  }

  /**
   * Creates a new Chat for the game with the specific number, saves it in the db and adds it to the
   * cache.
   *
   * @param number the number of the Chat.
   * @return the newly created and saved Chat
   */
  @Transactional
  public ChatEntity create(Long number) {
    ChatEntity result = chatRepository.save(new ChatEntity(number));
    currentChatMap.put(result.getNumber(), result);
    return result;
  }

  @Transactional
  public List<ChatEntity> save(List<ChatEntity> chats) {
    return chatRepository.saveAll(chats);
  }

  /**
   * Overwrites the existing cached chats with the ones from this game.
   */
  public void loadIntoCache() {

    currentChatMap = new HashMap<>();
    chatRepository.findAll().forEach(chat -> currentChatMap.put(chat.getNumber(), chat));
  }

  @Transactional
  ChatEntity save(ChatEntity chat) {
    return chatRepository.save(chat);
  }

  ChatEntity find(Long id) {
    return chatRepository.findById(id).orElseThrow();
  }

  ChatEntity find(UUID uuid) {
    return chatRepository.findByUuid(uuid).orElseThrow();
  }

  ChatEntity findByNumber(Long number) {
    return chatRepository.findByNumber(number).orElseThrow();
  }

  /**
   * Sends a message (and the metadata) from a user to a specific chat.
   *
   * @param account  the account of the user
   * @param number   the number of the chat
   * @param message  the message
   * @param metadata the metadata of the message
   * @return the message entity
   */
  public MessageEntity sendMessageToChat(AccountEntity account, Long number,
      String message, String metadata) {
    ChatEntity cachedChat = getChat(number);
    MessageEntity result = messageService.create(account, cachedChat, message, metadata);

    cachedChat.getMessages().add(0, result);

    if (cachedChat.getMessages().size() >= 50) {
      cachedChat.getMessages().remove(cachedChat.getMessages().size() - 1);
    }

    result = messageService.save(result);
    wsUtils.convertAndSendToTopic(ChatController.CHAT_UPDATE_DESTINATION + number,
        new MessageDTO(result));
    return result;
  }

  /**
   * Sends a message (and the metadata) from a user to all chats.
   *
   * @param account  the account of the user
   * @param message  the message
   * @param metadata the metadata of that message
   * @return the list of all the messages, sent to different chats
   */
  public List<MessageEntity> sendGlobalMessage(AccountEntity account, String message,
      String metadata) {
    return currentChatMap.values().stream()
        .map(chat -> sendMessageToChat(account, chat.getNumber(), message, metadata))
        .collect(Collectors.toList());
  }

  /**
   * gets the instance of a chat, if there is no cached chat, it will first look for a chat version
   * from the database or create on if there is none.
   *
   * @param number the number the chat has
   * @return the Chat Entity from the cache
   */
  public ChatEntity getChat(Long number) {
    if (currentChatMap.isEmpty()) {
      return null;
    }

    return currentChatMap.get(number);
  }
}
