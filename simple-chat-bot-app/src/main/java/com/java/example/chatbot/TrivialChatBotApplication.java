package com.java.example.chatbot;

import java.util.Scanner;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication()
public class TrivialChatBotApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TrivialChatBotApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ChatEngine chatEngine) {

        return args -> {

            try(Scanner scanner = new Scanner(System.in)) {

                String input;

                System.out.println("Welcome to the MemGTP Chatbot.  What is your user id?");
                System.out.print("=> ");
                input = scanner.nextLine();

                chatEngine.initialize(input);

                while (true) {
                    System.out.print("=> ");
                    input = scanner.nextLine();

                    if (input.equalsIgnoreCase("exit")) {
                        break; // Exit the loop if the user types "exit"
                    }

                    try {
                        String chatResult = chatEngine.chat(input);

                        System.out.println(chatResult);
                    }
                    catch(Exception e) {
                        System.err.println("Chat error occurred.  Please try again or come back and try again later: " + e.getMessage());
                    }
                }
            }
        };
    }

    public record AgentCreateRequest(String agentName, int contextWindowSize) {}

}
