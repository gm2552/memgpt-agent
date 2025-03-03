package com.example.memgptagent;


import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(exclude = {PgVectorStoreAutoConfiguration.class})
public class MemgptAgentApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(MemgptAgentApplication.class).web(WebApplicationType.SERVLET).run(args);
	}

}
