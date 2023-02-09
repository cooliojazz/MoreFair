package de.kaliburg.morefair.bots;

import de.kaliburg.morefair.FairConfig;
import de.kaliburg.morefair.account.AccountDetailsDto;
import de.kaliburg.morefair.account.AccountService;
import de.kaliburg.morefair.api.websockets.messages.WsObservedMessage;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import de.kaliburg.morefair.game.UpgradeUtils;
import de.kaliburg.morefair.game.round.LadderEntity;
import de.kaliburg.morefair.game.round.LadderService;
import de.kaliburg.morefair.game.round.LadderUtils;
import de.kaliburg.morefair.game.round.RankerEntity;
import de.kaliburg.morefair.game.round.RoundService;
import java.util.ArrayList;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Ricky
 */
@Service
public class BotService {

	final static String USER_DESTINATION = "/user/queue";
	final static String APP_DESTINATION = "/app";
	final static String TOPIC_DESTINATION = "/topic";
	
	@Value("${fair.max-bots}")
	private int MAX_BOTS;
	
	@Value("${fair.bot-connect-max-timeout}")
	private int CONNECTION_TIMEOUT;
	
    @Autowired
	private FairConfig config;
    
    @Autowired
    public BotRepository botRepo;
	
    @Autowired
    public AccountService accountService;
	
    @Autowired
    public LadderService ladderService;
	
    @Autowired
    public NameService nameService;
	
    @Autowired
    public RoundService roundService;
	
    @Autowired
	public LadderUtils ladderUtils;
	
    @Autowired
	public UpgradeUtils upgradeUtils;
	
	private final ArrayList<BotEntity> bots = new ArrayList<>();
	private final HashMap<BotEntity, StompSession> botSessions = new HashMap<>();
	
	private final WebSocketClient client = new StandardWebSocketClient();
	private final WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(Collections.singletonList(new WebSocketTransport(client))));
	
    @PostConstruct
    public void init() {
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		bots.addAll(botRepo.findAll());
	}
	
	private int timeout = 0;
    @Scheduled(fixedRate = 1000)
    public void botTick() {
		nameService.generate();
		long time = System.nanoTime();
		
		// Spawn new bots
		if (bots.size() < MAX_BOTS) {
			createBot();
		}
		
		// Open any missing connections
		if (timeout <= 0) {
			for (BotEntity bot : bots) {
				if (botSessions.get(bot) == null) {
					try {
						botSessions.put(bot, connect(bot));
						timeout += Math.random() * CONNECTION_TIMEOUT;
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			timeout--;
		}
		
		// Tick bots
		for (BotEntity bot : bots) {
			// Skip if not initialized
			if (!bot.isLoggedIn()) continue;
			
			LadderEntity ladder = ladderService.find(bot.getCurrentLadder());
			if (ladder == null) continue;
			RankerEntity ranker = ladder.getRankers().stream().filter(r -> r.getAccount().getId().equals(bot.getAccount().getId())).findFirst().orElse(null);
			if (ranker == null) continue;
			
			runBot(bot, ladder, ranker);
		}
		
//		botRepo.saveAll(bots);
		// Sooooo not entirely sure if this makes this sort of thread-safe, but hey at least its fast for now lol? 
		if ((System.currentTimeMillis() / 1000) % 5 == 0) new Thread(((Function<ArrayList<BotEntity>, Runnable>)(bs -> () -> {
					long sTime = System.nanoTime();
					botRepo.saveAll(bs);
					System.out.println("Save time: " + (System.nanoTime() - sTime) / 1000000000d);
				})).apply((ArrayList<BotEntity>)bots.clone())).start();
		
		System.out.println("Tick time: " + (System.nanoTime() - time) / 1000000000d + ", current timeout: " + timeout + ", " + bots.stream().filter(b -> b.isLoggedIn()).count() + " bots connected.");
    }
	
	private void createBot() {
		BotEntity bot = new BotEntity(UUID.randomUUID(), BotType.randomType(), nameService.generate());
		if (bot.getType() == BotType.ZOMBIE) bot.getData().put("waitTime", Math.random() * config.getManualPromoteWaitTime() + config.getManualPromoteWaitTime() / 2);
		if (bot.getType() == BotType.SLEEPYSPAMMER) {
			bot.getData().put("awake", true);
			bot.getData().put("wakeTime", Math.random() * 60 * 60);
			bot.getData().put("sleepTime", Math.random() * 60 * 60);
			bot.getData().put("time", 0);
		}
		if (bot.getType() == BotType.RANDOM) bot.getData().put("rate", Math.random() / 2 + 0.01);
		if (bot.getType() == BotType.FARMER) {
			bot.getData().put("patience", Math.random() * 60);
			bot.getData().put("lastTime", 0);
		}
		bots.add(bot);
	}
	
	private void runBot(BotEntity bot, LadderEntity ladder, RankerEntity ranker) {
		switch (bot.getType()) {
			case TRUEZOMBIE: {
				break;
			}
			case ZOMBIE: {
				if (Math.random() < 1 / bot.getData().get("waitTime").asDouble(1) && ladderUtils.canPromote(ladder, ranker)) sendGameEvent(bot, "promote");
				break;
			}
			case AUTOZOMBIE: {
				if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound())) sendGameEvent(bot, "autopromote");
				break;
			}
			case SPAMMER: {
				if (ladderUtils.canPromote(ladder, ranker)) {
					sendGameEvent(bot, "promote");
				} else if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0) {
					sendGameEvent(bot, "multi");
				} else if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0) {
					sendGameEvent(bot, "bias");
				} else if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound())) {
					sendGameEvent(bot, "autopromote");
				} else if (ladderUtils.canThrowVinegarAt(ladder, ranker, ladder.getRankers().get(0))) {
					sendGameEvent(bot, "vinegar");
				}
				break;
			}
			case SLEEPYSPAMMER: {
				bot.getData().put("time", bot.getData().get("time").asInt() + 1);
				if (bot.getData().get("awake").asBoolean(true)) {
					if (ladderUtils.canPromote(ladder, ranker)) {
						sendGameEvent(bot, "promote");
					} else if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0) {
						sendGameEvent(bot, "multi");
					} else if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0) {
						sendGameEvent(bot, "bias");
					} else if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound())) {
						sendGameEvent(bot, "autopromote");
					} else if (ladderUtils.canThrowVinegarAt(ladder, ranker, ladder.getRankers().get(0))) {
						sendGameEvent(bot, "vinegar");
					}
					if (bot.getData().get("time").asInt() > bot.getData().get("wakeTime").asInt()) {
						bot.getData().put("awake", false);
						bot.getData().put("time", 0);
					}
				} else {
					if (bot.getData().get("time").asInt() > bot.getData().get("sleepTime").asInt()) {
						bot.getData().put("awake", true);
						bot.getData().put("time", 0);
					}
				}
				break;
			}
			case SUPERSPAMMER: {
				if (ladderUtils.canPromote(ladder, ranker)) {
					sendGameEvent(bot, "promote");
				}
				if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0) {
					sendGameEvent(bot, "multi");
				}
				if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0) {
					sendGameEvent(bot, "bias");
				}
				if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound())) {
					sendGameEvent(bot, "autopromote");
				}
				if (ladderUtils.canThrowVinegarAt(ladder, ranker, ladder.getRankers().get(0))) {
					sendGameEvent(bot, "vinegar");
				}
				break;
			}
			case RANDOM: {
				double rate = bot.getData().get("rate").asDouble(0);
				if (Math.random() < rate) {
					sendGameEvent(bot, "promote");
				} else if (Math.random() < rate) {
					sendGameEvent(bot, "multi");
				} else if (Math.random() < rate) {
					sendGameEvent(bot, "bias");
				} else if (Math.random() < rate) {
					sendGameEvent(bot, "autopromote");
				} else if (Math.random() < rate) {
					sendGameEvent(bot, "vinegar");
				}
				break;
			}
			case RUNNERUP: {
				int mult = findHighestMult(ladder);
				if (ladderUtils.canPromote(ladder, ranker)) {
					sendGameEvent(bot, "promote");
				} else if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0 &&
						ranker.getMultiplier() < mult - 1) {
					sendGameEvent(bot, "multi");
				} else if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0 &&
						(ranker.getMultiplier() < mult - 2 || ranker.getBias() < findHighestBias(ladder) - 1)) {
					sendGameEvent(bot, "bias");
				} else if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound())) {
					sendGameEvent(bot, "autopromote");
				} else if (ladderUtils.canThrowVinegarAt(ladder, ranker, ladder.getRankers().get(0)) && ranker.getRank() == 2) {
					sendGameEvent(bot, "vinegar");
				}
				break;
			}
			case ANTIFIRST: {
				if (ladderUtils.canPromote(ladder, ranker)) {
					sendGameEvent(bot, "promote");
				} else if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0) {
					sendGameEvent(bot, "multi");
				} else if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0 && ranker.getRank() == 1) {
					sendGameEvent(bot, "bias");
				} else if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound())) {
					sendGameEvent(bot, "autopromote");
				} else if (ladderUtils.canThrowVinegarAt(ladder, ranker, ladder.getRankers().get(0)) && ranker.getRank() > 2) {
					sendGameEvent(bot, "vinegar");
				}
				break;
			}
			case WALL: {
				if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0) {
					sendGameEvent(bot, "multi");
				} else if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0 && ranker.getRank() == 1) {
					sendGameEvent(bot, "bias");
				}
				break;
			}
			case FARMER: {
				bot.getData().put("lastTime", bot.getData().get("lastTime").asInt() + 1);
				if (ladderUtils.canPromote(ladder, ranker)) {
					sendGameEvent(bot, "promote");
				} else {
					if (Math.random() > (bot.getData().get("patience").asDouble()- bot.getData().get("lastTime").asInt()) / bot.getData().get("patience").asDouble()) {
						if (ranker.getPoints().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getBias(), ladder.getTypes())) >= 0 &&
								ranker.getRank() < ladder.getRankers().size()) {
							sendGameEvent(bot, "bias");
							bot.getData().put("lastTime", 0);
						} else if (ranker.getPower().compareTo(upgradeUtils.buyUpgradeCost(ladder.getNumber(), ranker.getMultiplier(), ladder.getTypes())) >= 0 &&
								ranker.getRank() < ladder.getRankers().size()) {
							sendGameEvent(bot, "multi");
							bot.getData().put("lastTime", 0);
//						} else if (ladderUtils.canBuyAutoPromote(ladder, ranker, roundService.getCurrentRound()) && ranker.getRank() == ladder.getRankers().size()) {
//							sendGameEvent(bot, "autopromote");
		//				} else if (ladderUtils.canThrowVinegarAt(ladder, ranker, ladder.getRankers().get(0))) {
		//					sendGameEvent(bot, "vinegar");
						}
					}
				}
				break;
			}
		}
	}
	
	private int findHighestMult(LadderEntity ladder) {
		return -ladder.getRankers().stream().map(r -> -r.getMultiplier()).sorted().findFirst().orElse(0);
	}
	
	private int findHighestBias(LadderEntity ladder) {
		return -ladder.getRankers().stream().map(r -> -r.getBias()).sorted().findFirst().orElse(0);
	}
	
	private void sendGameEvent(BotEntity bot, String type) {
		synchronized (botSessions.get(bot)) {
			botSessions.get(bot).send(APP_DESTINATION + "/game/" + type, new WsObservedMessage(bot.getUuid().toString(), "", "{\"isTrusted\":true,\"screenX\":0,\"screenY\":0}"));
		}
	}
	
	private StompSession connect(BotEntity bot) throws InterruptedException, ExecutionException {
		return stompClient.connect("http://localhost:8080/fairsocket", new BotSessionHandler(this, bot)).get();
	}
	
	public StompSession getBotSession(BotEntity bot) {
		return botSessions.get(bot);
	}
	
	public void resetBotSession(BotEntity bot) {
		bot.setLoggedIn(false);
		botSessions.put(bot, null);
	}
	
	public void loginBot(BotEntity bot, AccountDetailsDto account) {
		bot.setUuid(account.getUuid());
		bot.setAccount(accountService.find(account.getAccountId()));
		bot.setCurrentLadder(account.getHighestCurrentLadder());
		bot.setLoggedIn(true);
		botRepo.save(bot);
	}
}
