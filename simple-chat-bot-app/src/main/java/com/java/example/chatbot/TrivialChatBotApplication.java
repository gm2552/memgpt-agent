package com.java.example.chatbot;

import java.util.List;
import java.util.Scanner;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
public class TrivialChatBotApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TrivialChatBotApplication.class).web(WebApplicationType.NONE).run(args);
    }


    @Bean
    public CommandLineRunner commandLineRunner(WebClient.Builder clientBuilder) {


        return args -> {

            Scanner scanner = new Scanner(System.in);
            String input;

            System.out.println("Welcome to the MemGTP Chatbot.  What is your user id?");
            System.out.print("=> ");
            input = scanner.nextLine();

            try {
                clientBuilder.build().post()
                        .uri("http://localhost:8080/agent", input)
                        .bodyValue(new AgentCreateRequest(input, 4026))
                        .retrieve().bodyToMono(String.class).block();
                System.out.println("Welcome.  Let's start a conversation.  What is your name?");

            }
            catch (WebClientResponseException e) {
                if (e.getStatusCode() != HttpStatusCode.valueOf(409)) {
                    throw new RuntimeException(e);
                }
                System.out.println("Welcome back.  Let's continue our conversation.");
            }

            final WebClient webClient = clientBuilder.baseUrl("http://localhost:8080/chat/" + input).build();


            while (true) {
                System.out.print("=> ");
                input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    break; // Exit the loop if the user types "exit"
                }

                OpenAiApi.ChatCompletionMessage msg =
                        new OpenAiApi.ChatCompletionMessage(input, OpenAiApi.ChatCompletionMessage.Role.USER);

                OpenAiApi.ChatCompletionRequest req = new OpenAiApi.ChatCompletionRequest(List.of(msg), "", .5);

                OpenAiApi.ChatCompletion completion = webClient.post().bodyValue(req).retrieve().bodyToMono(OpenAiApi.ChatCompletion.class).block();


                System.out.println(completion.choices().get(0).message().content());
                System.out.println("Context Window Usage: " + completion.usage().totalTokens());
            }
            scanner.close();

        };
    }

    public record AgentCreateRequest(String agentName, int contextWindowSize) {}

}
