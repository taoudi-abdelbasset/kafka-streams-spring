package com.example.demo.controllers;

import com.example.demo.events.PageEvent;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class PageEventController {
    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private InteractiveQueryService interactiveQueryService;
    @GetMapping("/publish")
    public PageEvent publish(String name,String topic){
        PageEvent event = new PageEvent(
                name,
                Math.random()>0.5?"U1":"U2",
                new Date(),
                10 + new Random().nextInt(10000)
        );

        streamBridge.send(topic,event);
        return event;
    }

    /**
     * Real-time analytics endpoint that streams aggregated page view counts
     *
     * HOW IT WORKS WITH kStreamFunction:
     * 1. kStreamFunction processes events and stores counts in "count-store" (a state store)
     * 2. This endpoint queries that SAME "count-store" to read the aggregated results
     * 3. Returns data as Server-Sent Events (SSE) - client gets updates every second
     *
     * THE FLOW:
     * - Every 1 second, this Flux emits a new map
     * - It queries the window store for the last 5 seconds of data
     * - Returns count of page views per page name in that 5-second window
     * - Client sees live updates without refreshing!
     */
    @GetMapping(path = "/analytics",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics(){
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence->{
                    Map<String,Long> stringLongMap=new HashMap<>();
                    ReadOnlyWindowStore<String, Long> windowStore = interactiveQueryService.getQueryableStore("count-store", QueryableStoreTypes.windowStore());
                    Instant now=Instant.now();
                    Instant from=now.minusMillis(5000);
                    KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now);
                    //WindowStoreIterator<Long> fetchAll = windowStore.fetch(page, from, now);
                    while (fetchAll.hasNext()){
                        KeyValue<Windowed<String>, Long> next = fetchAll.next();
                        stringLongMap.put(next.key.key(),next.value);
                    }
                    return stringLongMap;
                });
    }
}
