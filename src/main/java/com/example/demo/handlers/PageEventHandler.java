package com.example.demo.handlers;

import com.example.demo.events.PageEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @Bean
    public Supplier<PageEvent> pageEventSupplier(){
        return ()->{
            return new PageEvent(
                    Math.random()>0.5?"P1":"P2",
                    Math.random()>0.5?"U1":"U2",
                    new Date(),
                    10+new Random().nextInt(10000)
            );
        };
    }

    /**
     * Kafka Streams function that processes PageEvents and counts them by page name
     *
     * THE COMPLETE PIPELINE:
     *
     * 1. INPUT: Receives KStream<String, PageEvent> from topic T2
     *    - These are the events produced by pageEventSupplier every 800ms
     *
     * 2. FILTER: Keep only events where duration > 100
     *    - Filters out short page views (less than 100ms)
     *
     * 3. MAP: Transform PageEvent to KeyValue(pageName, duration)
     *    - Extract page name as key, duration as value
     *    - Example: PageEvent("P1", ..., 500) becomes KeyValue("P1", 500)
     *
     * 4. GROUP BY KEY: Group all events by page name
     *    - All "P1" events together, all "P2" events together
     *    - Specifies serializers for String keys and Long values
     *
     * 5. WINDOWING: Create 5-second tumbling windows
     *    - Groups events into non-overlapping 5-second buckets
     *    - Each window is independent (tumbling, not sliding)
     *
     * 6. COUNT: Count events per page per window
     *    - Materialized.as("count-store") creates a STATE STORE
     *    - This store is QUERYABLE by the analytics endpoint!
     *    - State store persists counts: {"P1": 15, "P2": 23} for each window
     *
     * 7. TO STREAM: Convert the windowed counts back to a stream
     *    - Converts KTable back to KStream for output
     *
     * 8. FINAL MAP: Flatten the Windowed key to just the page name
     *    - Input: KeyValue(Windowed("P1"), 15)
     *    - Output: KeyValue("P1", 15)
     *
     * 9. OUTPUT: Publishes to topic T3
     *    - Format: key="P1", value=15 (count of P1 page views in that window)
     *
     * CONNECTION TO ANALYTICS ENDPOINT:
     * - The "count-store" created here is THE SAME store that /analytics queries
     * - kStreamFunction continuously WRITES aggregated counts to the store
     * - /analytics endpoint continuously READS from the store every second
     * - This enables real-time dashboards without querying the Kafka topics directly!
     */
    @Bean
    public Function<KStream<String,PageEvent>, KStream<String,Long>> kStreamFunction(){
        return (input) ->
                input
                //.filter((k,v)->v.duration()>100)
                .map((k,v)->new KeyValue<>(v.name(), v.duration()))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
                .count(Materialized.as("count-store"))
                .toStream()
//                .peek((k,v)->{
//                    System.out.println("=======>  "+ k.key());
//                    System.out.println("=======>  "+v);
//                })
                .map((k,v)->new KeyValue<>(k.key(), v))
        ;
    }
}
