package de.kaliburg.morefair.controller;

import de.kaliburg.morefair.account.entity.Account;
import de.kaliburg.morefair.account.service.AccountService;
import de.kaliburg.morefair.dto.ChatDTO;
import de.kaliburg.morefair.messages.WSMessage;
import de.kaliburg.morefair.moderation.controller.ModerationController;
import de.kaliburg.morefair.moderation.data.ModChatMessageData;
import de.kaliburg.morefair.persistence.entity.Message;
import de.kaliburg.morefair.service.MessageService;
import de.kaliburg.morefair.service.RankerService;
import de.kaliburg.morefair.utils.WSUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import java.util.UUID;

@Log4j2
@Controller
public class ChatController {
    public static final String CHAT_DESTINATION = "/queue/chat/";
    public static final String CHAT_UPDATE_DESTINATION = "/topic/chat/";
    private final MessageService messageService;
    private final AccountService accountService;
    private final RankerService rankerService;
    private final WSUtils wsUtils;

    public ChatController(MessageService messageService, AccountService accountService, RankerService rankerService, WSUtils wsUtils) {
        this.messageService = messageService;
        this.accountService = accountService;
        this.rankerService = rankerService;
        this.wsUtils = wsUtils;
    }

    @MessageMapping("/chat/init/{number}")
    public void initChat(SimpMessageHeaderAccessor sha, WSMessage wsMessage, @DestinationVariable("number") Integer number) {
        try {
            String uuid = StringEscapeUtils.escapeJava(wsMessage.getUuid());
            log.debug("/app/chat/init/{} from {}", number, uuid);
            Account account = accountService.findAccountByUUID(UUID.fromString(uuid));
            if (account == null) wsUtils.convertAndSendToUser(sha, CHAT_DESTINATION, HttpStatus.FORBIDDEN);
            if (number <= rankerService.findHighestRankerByAccount(account).getLadder().getNumber()) {
                ChatDTO c = messageService.getChat(number);
                wsUtils.convertAndSendToUser(sha, CHAT_DESTINATION, c);
            } else {
                wsUtils.convertAndSendToUser(sha, CHAT_DESTINATION, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IllegalArgumentException e) {
            wsUtils.convertAndSendToUser(sha, CHAT_DESTINATION, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            wsUtils.convertAndSendToUser(sha, CHAT_DESTINATION, HttpStatus.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat/post/{number}")
    public void postChat(WSMessage wsMessage, @DestinationVariable("number") Integer number) {
        try {
            String uuid = StringEscapeUtils.escapeJava(wsMessage.getUuid());
            String message = wsMessage.getContent();
            message = message.trim();
            if (message.length() > 280) message = message.substring(0, 280);
            message = StringEscapeUtils.escapeJava(HtmlUtils.htmlEscape(message));

            log.debug("/app/chat/post/{} '{}' from {}", number, message, uuid);
            Account account = accountService.findAccountByUUID(UUID.fromString(uuid));
            if (account == null) return;
            if (number <= rankerService.findHighestRankerByAccount(account).getLadder().getNumber()) {
                Message answer = messageService.writeMessage(account, number, message);
                wsUtils.convertAndSendToAll(CHAT_UPDATE_DESTINATION + number, answer.convertToDTO());
                wsUtils.convertAndSendToAll(ModerationController.CHAT_UPDATE_DESTINATION, new ModChatMessageData(answer));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
