package de.kaliburg.morefair.bots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 *
 * @author Ricky
 */
@Service
public class NameService {
	
	private String[] words;
	
	@PostConstruct
	public void init() throws IOException {
		words = new BufferedReader(new InputStreamReader(new ClassPathResource("words_alpha.txt").getInputStream()))
				.lines().filter(w -> w.length() > 1 && w.length() < 8).toArray(String[]::new);
	}
	
	public String generate() {
		String name = "";
		int size = 1 + (int)(Math.random() * 3);
		for (int i = 0; i < size; i++) {
			String part = words[(int)(Math.random() * words.length)];
			if (Math.random() < 0.67) part = part.substring(0, 1).toUpperCase() + part.substring(1);
			name += (name.length() > 0 && Math.random() < 0.67 ? " " : "") + part;
		}
		return name;
	}
}
