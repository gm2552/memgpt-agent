package com.example.memgptagent;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication()
public class MemgptAgentApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(MemgptAgentApplication.class).web(WebApplicationType.SERVLET).run(args);
	}

}
