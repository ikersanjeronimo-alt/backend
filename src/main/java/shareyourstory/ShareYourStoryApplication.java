package shareyourstory;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShareYourStoryApplication {
	public static void main(String[] args) {
		// La JVM del contenedor corre en UTC por defecto, asi que LocalDateTime.now()
		// daba la hora dos horas atrasada (las marcas de tiempo del chat, botellas,
		// etc. salian en UTC). Fijamos la zona de la app a Espana antes de arrancar
		// para que toda hora generada con LocalDateTime.now() sea hora local (con DST).
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
		SpringApplication.run(ShareYourStoryApplication.class, args);
	}
}
