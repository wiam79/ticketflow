# TicketFlow

**A hands-on reference project for learning Spring Cloud and microservices, built around a real-world ticket booking platform.**

TicketFlow is an open-source educational project that takes you from
"what is a microservice?" to building resilient, event-driven, observable
distributed systems with the Spring ecosystem.

Instead of yet another e-commerce demo, it tackles problems that naturally
require distributed-systems patterns: limited seat inventory, concurrent
bookings, unreliable payment providers, and asynchronous notifications.

## What you will learn

- **Service discovery & routing**: Eureka and Spring Cloud Gateway
- **Centralized configuration**: Spring Cloud Config with dynamic refresh
- **Inter-service communication**: declarative REST clients with OpenFeign
- **Resilience**: circuit breaker, retry, timeout, bulkhead and fallbacks (Resilience4J)
- **Event-driven architecture**: asynchronous messaging with Apache Kafka
- **Distributed transactions**: Saga pattern, Outbox pattern, idempotency
- **Containerization**: Docker and Docker Compose
- **AI integration** *(later)*: natural-language search and RAG assistant with Spring AI

## Architecture

| Module | Role | Key technology | Status |
|---|---|---|---|
| `customer-service` | Customer profiles (CRUD) | Spring Web, JPA | Done |
| `api-gateway` | Single entry point, routing, rate limiting | Spring Cloud Gateway | Done |
| `discovery-server` | Service registry | Eureka | Done |
| `event-service` | Events and seats catalog | Spring Web, JPA | Planned |
| `config-server` | Centralized configuration from GitHub | Spring Cloud Config | Done (TP3) |
| `booking-service` | Booking and seat locking | OpenFeign, Resilience4J | Planned |
| `payment-service` | Simulated payment provider (random failures) | Resilience4J | Planned |
| `notification-service` | Confirmation emails/SMS | Kafka consumer | Planned |
| `ai-assistant-service` | Natural-language search, recommendations | Spring AI | Later |

## Roadmap

Each step has a Git tag and a lesson in `docs/lessons/`.

| Tag | Content |
|---|---|
| `step-00-setup` | Parent POM, repo structure, CI |
| `step-01-customer-service` | First business service |
| `step-02-gateway-static` | Gateway with static routes |
| `step-03-discovery` | Eureka + load-balanced routing |
| `step-04-event-service` | Second business service |
| `step-05-config` | Config Server |
| `step-06-booking-feign` | booking-service + OpenFeign |
| `step-07-resilience` | payment-service + Resilience4J |
| `step-08-kafka` | Events + notification-service |
| `step-09-saga` | Saga, Outbox, idempotency |
| `step-10-security` | Keycloak (customer-service integration) |
| `step-11-observability` | Tracing and metrics |
| `step-12-ai` | Spring AI |

To follow a lesson, check out its tag: `git checkout step-01-customer-service`.

## Tech stack

Java 21 · Spring Boot 4 · Spring Cloud 2025.1 · Resilience4J · Apache Kafka ·
PostgreSQL · Docker / Docker Compose · Spring AI (later)

## Repository structure

    ticketflow/
    ├── docs/            # lessons, ADRs, templates
    ├── infra/           # docker-compose and infrastructure config
    ├── config-repo/     # optional local examples; TP3 uses ticketflow-config
    └── services/        # one folder per microservice, each with its own README

Every module is independently runnable and testable, and documents itself in
its own `README.md`.

## Getting started

Prerequisites: JDK 21, Git, Docker, and network access for the included Maven Wrapper.

    git clone https://github.com/<your-user>/ticketflow.git
    cd ticketflow
    ./mvnw clean verify

## TP3 — Centralized configuration

TP3 continues from the `tp2-discovery-service` branch. The application code is in
[`wiam79/ticketflow`](https://github.com/wiam79/ticketflow/tree/tp3-config-server)
on branch `tp3-config-server`; the configuration repository is
[`wiam79/ticketflow-config`](https://github.com/wiam79/ticketflow-config), branch
`main`. These are separate repositories. The application repository's `main`
branch remains the project baseline.

### Prerequisites and checkout

Use JDK 21, Git, network access to GitHub and Maven Central, and PowerShell. Maven
is supplied by the checked-in Wrapper; do not substitute a global Maven install.

```powershell
git clone --branch tp3-config-server https://github.com/wiam79/ticketflow.git
git clone https://github.com/wiam79/ticketflow-config.git
Set-Location .\ticketflow
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

The two clones should be siblings. The configuration examples are public and
contain no credentials. A professor with read access can clone and run the
published files. To demonstrate changing v1 to v2 or to publish a new profile,
the user performing that step needs push permission to `ticketflow-config`; a
professor without that permission can use their own fork and point Config Server
to it. Never place a GitHub token in either repository.

### Start the services

Open four PowerShell terminals in the application checkout. Define the Git URI
in the Config Server terminal itself, then start services in this order:

```powershell
# Terminal 1 — Config Server, port 8888
$env:CONFIG_GIT_URI = 'https://github.com/wiam79/ticketflow-config.git'
.\mvnw.cmd -pl services/config-server spring-boot:run
```

```powershell
# Terminal 2 — Eureka, port 8761
.\mvnw.cmd -pl services/discovery-server spring-boot:run
```

```powershell
# Terminal 3 — Customer Service, port 8081
.\mvnw.cmd -pl services/customer-service spring-boot:run
```

```powershell
# Terminal 4 — API Gateway, port 8080
.\mvnw.cmd -pl services/api-gateway spring-boot:run
```

Environment variables are per terminal/process: defining `CONFIG_GIT_URI` in a
different PowerShell window does not configure Config Server. Wait for each
application to finish starting and allow Eureka a few seconds to register the
two clients. Config Server does not register with Eureka.

### Verify the configuration and CRUD route

Run these from any PowerShell window after startup:

```powershell
curl.exe -i http://localhost:8888/actuator/health
curl.exe -i http://localhost:8888/customer-service/default/main
curl.exe -i http://localhost:8888/api-gateway/default/main
curl.exe -i http://localhost:8761/eureka/apps
curl.exe -i http://localhost:8081/api/config/message
curl.exe -i http://localhost:8080/actuator/gateway/routes
```

The Config Server responses should include the shared Eureka settings and the
service-specific port, route, message, Actuator exposure, and Git commit version.
The gateway route is explicitly `lb://customer-service` for
`/api/customers/**`; its Locator is disabled. The direct message endpoint is
available on 8081 only. `/api/config/message` and `/actuator/refresh` are not
Gateway routes, so their absence on port 8080 is expected.

```powershell
$body = '{"firstName":"Salma","lastName":"Alaoui","email":"salma.tp3@example.com"}'
curl.exe -i -X POST http://localhost:8080/api/customers -H 'Content-Type: application/json' --data-raw $body
curl.exe -i http://localhost:8080/api/customers
```

The POST should return `201 Created`, a `Location` header and
`X-Served-By: api-gateway`. The list should contain the created customer.
Choose a new email if the sample address is already present.

### Demonstrate v1 → v2 refresh without restarting

This demonstration requires push access to the configuration repository. In a
separate PowerShell window, from the sibling `ticketflow-config` checkout:

1. Confirm the service message at `http://localhost:8081/api/config/message` is
   `Bienvenue dans TicketFlow - configuration v1`. Create a customer via the
   gateway and keep its returned ID.
2. Change only `ticketflow.message` in `customer-service.yml` to
   `Bienvenue dans TicketFlow - configuration v2`, then run:

   ```powershell
   git add customer-service.yml
   git commit -m "Modifier le message TicketFlow"
   git push origin main
   ```

3. Read `http://localhost:8888/customer-service/default/main` and confirm the
   new `version` SHA and v2 property. Customer Service should still return v1
   before refresh; a Git push does not update an already running client.
4. Refresh that client directly, then read its message again:

   ```powershell
   curl.exe -i -X POST http://localhost:8081/actuator/refresh
   curl.exe -i http://localhost:8081/api/config/message
   curl.exe -i http://localhost:8080/api/customers
   ```

   The refresh response should include `ticketflow.message`; the direct message
   should become v2 and the customer with the saved ID should still exist. Do
   not stop or restart Customer Service between the v1 and v2 reads.

### Test the dev profile

After the refresh demonstration, add `customer-service-dev.yml` at the root of
`ticketflow-config` with:

```yaml
ticketflow:
  message: "TicketFlow - profil dev"
```

Commit and push it to `main` (push permission required), then query
`http://localhost:8888/customer-service/dev/main`. Stop only Customer Service,
and start it in a terminal where Config Server remains reachable:

```powershell
.\mvnw.cmd '-Dspring-boot.run.profiles=dev' -pl services/customer-service spring-boot:run
curl.exe -i http://localhost:8081/api/config/message
```

The result should be `TicketFlow - profil dev`. Restart Customer Service without
the profile option to return to the default profile. H2 is in-memory and uses
`ddl-auto=create-drop`, so restarting Customer Service clears its customers.

### Component roles and troubleshooting

- GitHub `ticketflow-config`: versioned shared and per-service configuration;
- Config Server `8888`: clones/reads that Git branch and serves configuration;
- Eureka `8761`: registers `customer-service` and `api-gateway`;
- API Gateway `8080`: forwards the customer CRUD route to Eureka's
  `customer-service` instance;
- Customer Service `8081`: CRUD plus the direct refresh-scope message endpoint.

If Config Server does not start, set `CONFIG_GIT_URI` in its own terminal and
check that `main` exists and the repository is readable. If a client refuses to
start, verify Config Server is healthy: the Config import is intentionally
mandatory. If configuration returns 200 but a property is missing, check the
exact filename (`customer-service.yml`, `api-gateway.yml`) and inspect
`propertySources`. If Gateway returns 503, verify Eureka lists
`CUSTOMER-SERVICE` as `UP` and allow registry propagation. For private forks,
configure JGit credentials locally using environment variables; never commit a
token. The refresh updates only the contacted Customer Service instance and is
for the refresh-scoped message bean, not ports, database connections or Gateway
routes. The shared configuration also sets `eureka.client.refresh.enable=false`
so refreshing the message does not unregister and recreate the Eureka client.

The published default configuration should end with the v1 message so the
professor can replay the demonstration. The v2 commit and the dev-profile commit
remain in Git history; restore v1 with a normal commit after the demonstration,
never by rewriting history.

## Who is this for?

Java developers who know Spring Boot basics and want to move into
microservices, and anyone preparing for backend or architecture interviews.

<!--
## Contributing

Contributions, issues and exercises are welcome. A `CONTRIBUTING.md` will be
added soon.
-->

## License

To be defined (MIT recommended for an educational project).

## Author

**AHAIDOUS Khadija**   | [GitHub](https://github.com/kahaidous>) · [LinkedIn](https://www.linkedin.com/in/khadijaahaidous)
