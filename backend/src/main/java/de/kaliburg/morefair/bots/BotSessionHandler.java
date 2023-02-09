package de.kaliburg.morefair.bots;

import de.kaliburg.morefair.api.AccountController;
import de.kaliburg.morefair.api.FairController;
import de.kaliburg.morefair.api.GameController;
import de.kaliburg.morefair.api.websockets.messages.WsEmptyMessage;
import de.kaliburg.morefair.api.websockets.messages.WsMessage;
import de.kaliburg.morefair.events.Event;
import de.kaliburg.morefair.events.types.EventType;
import java.lang.reflect.Type;
import java.util.HashMap;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

/**
 *
 * @author Ricky
 */
public class BotSessionHandler implements StompSessionHandler {
		
	private final BotService botService;
	private final BotEntity bot;
	private StompSession.Subscription ladderSubscription;

	public BotSessionHandler(BotService botService, BotEntity bot) {
		this.botService = botService;
		this.bot = bot;
	}

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		session.subscribe(subscribeHeaders(BotService.USER_DESTINATION + AccountController.QUEUE_LOGIN_DESTINATION), this);
		session.send(BotService.APP_DESTINATION + AccountController.APP_LOGIN_DESTINATION, new WsEmptyMessage(bot.getUuid().toString()));
	}

	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
		exception.printStackTrace();
	}

	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
		exception.printStackTrace();
		botService.resetBotSession(bot);
	}

	@Override
	public Type getPayloadType(StompHeaders headers) {
		if (headers.getDestination().equals(BotService.USER_DESTINATION + AccountController.QUEUE_LOGIN_DESTINATION)) return WsWrapper.AccountDetailsDto.class;
		if (headers.getDestination().startsWith(BotService.TOPIC_DESTINATION + GameController.TOPIC_EVENTS_DESTINATION.replace("{number}", "")) ||
				headers.getDestination().equals(BotService.TOPIC_DESTINATION + GameController.TOPIC_GLOBAL_EVENTS_DESTINATION))
			return Event.class;
		return HashMap.class;
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		if (headers.getDestination() == null) {
			System.out.println("Got throttled?");
			return;
		}

		StompSession session = botService.getBotSession(bot);
		if (session == null) {
			System.out.println("Uhhhhh how???");
		}
		synchronized (session) {

			if (headers.getDestination().equals(BotService.USER_DESTINATION + AccountController.QUEUE_LOGIN_DESTINATION)) {
				botService.loginBot(bot, ((WsWrapper.AccountDetailsDto)payload).getContent());
				session.send(BotService.APP_DESTINATION + AccountController.APP_RENAME_DESTINATION, new WsMessage(bot.getUuid().toString(), bot.getName() + " " + bot.getType().getIcon() + "🤖"));
				session.subscribe(subscribeHeaders(BotService.USER_DESTINATION + FairController.QUEUE_INFO_DESTINATION), this);
				session.send(BotService.APP_DESTINATION + FairController.APP_INFO_DESTINATION, new WsEmptyMessage(bot.getUuid().toString()));
			}

			if (headers.getDestination().equals(BotService.USER_DESTINATION + FairController.QUEUE_INFO_DESTINATION)) {
//				session.subscribe(subscribeHeaders(USER_DESTINATION + GameController.QUEUE_INIT_DESTINATION), this);
				session.subscribe(subscribeHeaders(BotService.TOPIC_DESTINATION + GameController.TOPIC_GLOBAL_EVENTS_DESTINATION), this);
				ladderSubscription = session.subscribe(subscribeHeaders(BotService.TOPIC_DESTINATION + GameController.TOPIC_EVENTS_DESTINATION.replace("{number}", bot.getCurrentLadder() + "")), this);
				session.send(BotService.APP_DESTINATION + GameController.APP_INIT_DESTINATION.replace("{number}", bot.getCurrentLadder() + ""), new WsEmptyMessage(bot.getUuid().toString()));
			}

//			if (headers.getDestination().equals(USER_DESTINATION + GameController.QUEUE_INIT_DESTINATION)) {
//				System.out.println(payload);
//			}

			if (headers.getDestination().equals(BotService.TOPIC_DESTINATION + GameController.TOPIC_EVENTS_DESTINATION.replace("{number}", bot.getCurrentLadder() + ""))) {
				Event event = (Event)payload;
				if (event.getEventType() == EventType.PROMOTE && event.getAccountId().equals(bot.getAccount().getId())) {
					ladderSubscription.unsubscribe();
					bot.setCurrentLadder(bot.getCurrentLadder() + 1);
					ladderSubscription = session.subscribe(subscribeHeaders(BotService.TOPIC_DESTINATION + GameController.TOPIC_EVENTS_DESTINATION.replace("{number}", bot.getCurrentLadder() + "")), this);
				}
			}
			if (headers.getDestination().equals(BotService.TOPIC_DESTINATION + GameController.TOPIC_GLOBAL_EVENTS_DESTINATION)) {
				Event event = (Event)payload;
				if (event.getEventType() == EventType.RESET) {
					botService.resetBotSession(bot);
					session.disconnect();
				}
			}

			//System.out.println("Handling response on " + headers.getDestination());
		}
	}

	private StompHeaders subscribeHeaders(String destination) {
		StompHeaders subHeaders = new StompHeaders();
		subHeaders.setDestination(destination);
		subHeaders.add("uuid", bot.getUuid().toString());
		return subHeaders;
	}
}
