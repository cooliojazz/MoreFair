package de.kaliburg.morefair.utils;

import java.util.concurrent.Semaphore;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Ricky
 */
public class AutoCloseableSemaphore extends Semaphore implements AutoCloseable {

	public AutoCloseableSemaphore(int permits) {
		super(permits);
	}

	public AutoCloseableSemaphore(int permits, boolean fair) {
		super(permits, fair);
	}

	public AutoCloseableSemaphore safeAcquireGet(Logger log) {
		try {
			acquire();
		} catch (InterruptedException e) {
		  log.error(e.getMessage());
		  e.printStackTrace();
		  return null;
		}
		return this;
	}

	@Override
	public void close() {
		release();
	}
	
}
