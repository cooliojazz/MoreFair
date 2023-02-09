package de.kaliburg.morefair.bots;

import java.util.TreeMap;
import lombok.Getter;

/**
 *
 * @author Ricky
 */
@Getter
public enum BotType {
	TRUEZOMBIE("⛓️🧟", 0.075), AUTOZOMBIE("🚗🧟", 0.075), ZOMBIE("🧟 ", 0.1),
	SPAMMER("🖱 ", 0.15), SLEEPYSPAMMER("💤🖱", 0.2), SUPERSPAMMER("⏫🖱", 0.05),
	RANDOM("🎲 ", 0.25), RUNNERUP("➖1️", 0.2), ANTIFIRST("🚫1️", 0.2),
	WALL("🧱 ", 0.1), FARMER("🍇 ", 0.1);
	
	private String icon;
	private double chance;

	private BotType(String icon, double chance) {
		this.icon = icon;
		this.chance = chance;
	}
	
	private static final TreeMap<Double, BotType> typePool = new TreeMap<>();
	private static double total = 0;
	static {
		for (BotType type : values()) typePool.put(total += type.chance, type);
	}
	
	public static BotType randomType() {
		return typePool.ceilingEntry(Math.random() * total).getValue();
	}
}
