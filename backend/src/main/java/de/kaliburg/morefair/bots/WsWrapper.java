package de.kaliburg.morefair.bots;

import lombok.Data;

/**
 *
 * @author Ricky
 */
@Data
public class WsWrapper<T> {
	
	private T content;
	
	public static final class AccountDetailsDto extends WsWrapper<de.kaliburg.morefair.account.AccountDetailsDto> {}
}
