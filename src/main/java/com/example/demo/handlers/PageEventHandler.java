package com.example.demo.handlers;

import com.example.demo.events.PageEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PageEventHandler {
    @Bean
    public Consumer<PageEvent> pageEventConsumer(){
        return (input)->{
            System.out.println("**********************");
            System.out.println(input.date());
            System.out.println("**********************");
        };
    }
}
