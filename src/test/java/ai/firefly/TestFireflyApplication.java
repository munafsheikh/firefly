package ai.firefly;

import org.springframework.boot.SpringApplication;

public class TestFireflyApplication {

	public static void main(String[] args) {
		SpringApplication.from(FireflyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
