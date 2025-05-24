package hanium.modic.backend.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class KeyGenerator {

	private static final DateTimeFormatter FORMAT_YYYYMMDDHHMMSS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	public String generateKey() {
		return generateDate() + "_" + generateUUID();
	}

	private String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private String generateDate() {
		return FORMAT_YYYYMMDDHHMMSS.format(LocalDate.now());
	}
}
