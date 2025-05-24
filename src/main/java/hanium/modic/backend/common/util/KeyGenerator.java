package hanium.modic.backend.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class KeyGenerator {

	private static final DateTimeFormatter FORMAT_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

	public String generateKey() {
		return generateDate() + "_" + generateUUID();
	}

	private String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private String generateDate() {
		return FORMAT_YYYYMMDD.format(LocalDate.now());
	}
}
