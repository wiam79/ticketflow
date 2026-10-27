package com.ticketflow.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTests {

    @Autowired
    RouteLocator routeLocator;

    @Autowired
    DiscoveryClient discoveryClient;

    @Test
    void explicitCustomerRouteUsesLoadBalancedTestInstance() {
        var customerRoute = routeLocator.getRoutes()
                .filter(route -> route.getId().equals("customer-service"))
                .blockFirst();

        assertThat(customerRoute).isNotNull();
        assertThat(customerRoute.getUri().toString()).isEqualTo("lb://customer-service");
        assertThat(Mono.from(customerRoute.getPredicate().apply(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/customers/1").build()))).block()).isTrue();
        assertThat(discoveryClient.getInstances("customer-service"))
                .extracting(ServiceInstance::getUri)
                .contains(URI.create("http://localhost:12345"));
    }
}
