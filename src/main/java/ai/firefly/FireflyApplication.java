package ai.firefly;

import ai.firefly.dashboard.DashboardRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(DashboardRuntimeHints.class)
public class FireflyApplication {

	public static void main(String[] args) {
		SpringApplication.run(FireflyApplication.class, args);
	}

}
