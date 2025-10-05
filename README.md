# Kafka Streams Spring Boot Application

This repository demonstrates an event-driven architecture using Spring Boot with Kafka Streams. It simulates page view events, processes them in real-time, and provides analytics on page visits. The application includes:

- A **supplier** that generates random page events periodically.
- A **consumer** that logs incoming events.
- A **Kafka Streams processor** that filters, aggregates, and counts page views in tumbling windows (every 5 seconds).
- REST endpoints to manually publish events and stream real-time analytics.

The purpose is to showcase how to build a simple real-time data pipeline with Kafka for processing streaming data, such as counting page views per page in fixed time windows. This can be extended for web analytics, monitoring, or any stream processing use case.

## Prerequisites

- Java 17+ (for Spring Boot 3.x).
- Maven (for building the project).
- Kafka broker running locally (e.g., via Docker Compose). The app is configured for `localhost:9092`.
- Docker (optional but recommended for Kafka setup).


## I. Overview

This application showcases a complete Kafka Streams pipeline that:
- **Supplier** page view events automatically every 200ms
- **Processes** events using Kafka Streams with windowed aggregations
- **Visualizes** real-time analytics through a live dashboard
- **Demonstrates** stateful stream processing with queryable state stores

## II.️ Architecture

```mermaid
flowchart LR
    A[PageEvent Generation (T2)] --> B[Stream Processing]
    B --> C[Aggregated Counts (T3)]
    C --> D[Real-time Dashboard]

    B --> E[State Store (count-store)]
    E --> F[Analytics Endpoint (/analytics)]
```

### 1. Components

1. **PageEvent Producer** (`pageEventSupplier`)
    - Generates random page view events every 200ms
    - Events contain: page name (P1/P2), user (U1/U2), timestamp, duration
    - Publishes to topic `T2`

2. **Kafka Streams Processor** (`kStreamFunction`)
    - Consumes from topic `T2`
    - Groups events by page name
    - Applies 5-second tumbling windows
    - Counts page views per window
    - Stores results in queryable state store `count-store`
    - Outputs aggregated counts to topic `T3`

3. **Analytics REST API** (`/analytics`)
    - Queries the `count-store` state store
    - Streams real-time metrics via Server-Sent Events (SSE)
    - Updates clients every second

4. **Live Dashboard** (`index.html`)
    - Real-time chart visualization using SmoothieChart
    - Displays page view counts for P1 and P2
    - Auto-updates via SSE connection

## III. Getting Started
### 1. Start Kafka Infrastructure

```bash
# Navigate to project directory
cd "Activité Pratique N1 - Event Driven Architecture avec KAFKA"

# Start Zookeeper and Kafka broker
docker-compose up -d

# Verify services are running
docker-compose ps
```
> Note:
> Test if the consumer and producer working

![imgs/Pasted image.png]

### 2. Run the Application

```bash
cd demo

# Build and run
mvn spring-boot:run

# Or build JAR and run
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

![imgs/Pasted image (2).png]

The application will start on `http://localhost:8080`

![imgs/Pasted image (3).png]

## IV. Testing Features

### 1. Monitor Raw Events (Topic T2)

Watch the raw page events being generated:

```bash
docker exec -it bdcc-kafla-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic T2 \
  --from-beginning
```

**Expected Output:**
```html
{"name":"P1","user":"U2","date":1234567890,"duration":5432}
{"name":"P2","user":"U1","date":1234567891,"duration":8765}
```

![imgs/Pasted image (5).png]

### 2. Monitor Aggregated Counts (Topic T3)

Watch the windowed aggregation results:

```bash
docker exec -it bdcc-kafla-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic T3 \
  --from-beginning
```

**Expected Output:**
```
P1    15
P2    23
P1    18
```
![imgs/Pasted image (8).png]

### 3. Manual Event Publishing

Publish custom events via REST API:

```bash
# Publish to any topic
curl "http://localhost:8080/publish?name=P1&topic=T2"
```

**Response:**
```json
{
  "name": "P1",
  "user": "U2",
  "date": "2025-10-05T10:30:00.000+00:00",
  "duration": 4532
}
```

### 4. Real-time Analytics API

Stream live metrics via Server-Sent Events:

```bash
# Terminal monitoring
curl -N http://localhost:8080/analytics
```

**Response (streaming every 1 second):**
```
data:{"P1":15,"P2":23}

data:{"P1":18,"P2":20}

data:{"P1":22,"P2":25}
```

### 5. Live Dashboard

Open the real-time visualization dashboard:

```
http://localhost:8080/index.html
```

![imgs/Pasted image (9).png]

**Features:**
- Green line: P1 page view counts
- Red line: P2 page view counts
- Updates every second
- 5-second rolling window aggregation

## V. Application Flow

### Data Pipeline

1. **Event Generation** (every 200ms)
   ```
   PageEvent(name="P1", user="U1", date=now, duration=random)
   → Topic T2
   ```

2. **Stream Processing**
   ```
   Topic T2 → Filter → Map → GroupBy → Window(5s) → Count
   → State Store ("count-store")
   → Topic T3
   ```

3. **Real-time Querying**
   ```
   State Store ("count-store")
   ← Query (every 1s)
   ← /analytics endpoint
   ← Dashboard (via SSE)
   ```

## VI. Configuration

Key configurations in `application.properties`:

```properties
# Kafka broker
spring.cloud.stream.kafka.binder.brokers=localhost:9092

# Event generation rate (200ms)
spring.cloud.stream.bindings.pageEventSupplier-out-0.producer.poller.fixed-delay=200

# Topic bindings
pageEventConsumer: T1 (input)
pageEventSupplier: T2 (output)
kStreamFunction: T2 (input) → T3 (output)

# Stream processing commit interval
spring.cloud.stream.kafka.streams.binder.configuration.commit.interval.ms=1000
```

## VII. Dependencies

- **Spring Boot 3.4.2** - Application framework
- **Spring Cloud Stream 2025.0.0** - Stream binding abstraction
- **Kafka Streams** - Stream processing engine
- **Spring Kafka** - Kafka integration
- **Lombok** - Boilerplate code reduction

## VIII. Development History

Project commits demonstrate incremental development:
1. Basic PageEvent class and REST controller
2. Simple consumer for topic T1
3. Supplier for automatic event generation
4. Kafka Streams processor with windowing
5. Analytics endpoint with state store queries
6. Real-time dashboard with live charts

## IX. Use Cases

This application demonstrates:
- **Real-time Analytics**: Process streams and query results instantly
- **Event-Driven Architecture**: Loosely coupled microservices communication
- **Stateful Stream Processing**: Maintain aggregations with windowing
- **Interactive Queries**: Query Kafka Streams state stores via REST API
- **Live Dashboards**: Push real-time updates to web clients via SSE

## X. License

Demo project for educational purposes.