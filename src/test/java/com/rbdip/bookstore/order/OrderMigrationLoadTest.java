package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.target=3")
class OrderMigrationLoadTest {

    private static final int WORKERS = 4;
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_load")
            .withUsername("bookstore")
            .withPassword("bookstore");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void servesRequestsWhileContractMigrationRuns() throws Exception {
        TestRestTemplate client = createClient();
        Long productId = createProduct(client);
        insertLegacyOrder();
        Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger requestNumber = new AtomicInteger();
        CountDownLatch requestsStarted = new CountDownLatch(WORKERS);
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS + 1);

        for (int worker = 0; worker < WORKERS; worker++) {
            executor.submit(() -> sendRequests(
                    client, productId, running, requestNumber, requestsStarted, errors));
        }

        assertThat(requestsStarted.await(10, TimeUnit.SECONDS)).isTrue();
        Future<?> migration = executor.submit(() -> migrateContract(errors));
        migration.get(20, TimeUnit.SECONDS);
        running.set(false);
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(requestNumber.get()).isPositive();
        assertThat(errors).isEmpty();
    }

    private TestRestTemplate createClient() {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(2));
        return new TestRestTemplate(builder);
    }

    private Long createProduct(TestRestTemplate client) {
        ResponseEntity<Map> response = client.postForEntity(
                "/products", Map.of("name", "Migration Test", "price", new BigDecimal("10.00")), Map.class);
        return Long.valueOf(response.getBody().get("id").toString());
    }

    private void insertLegacyOrder() {
        Long orderId = jdbcTemplate.queryForObject(
                "INSERT INTO orders (customer_full_name, customer_address, status) "
                        + "VALUES ('Legacy User', 'Legacy Address', 'new') RETURNING id",
                Long.class);
        jdbcTemplate.update(
                "INSERT INTO order_items (order_id, product_name, product_price, quantity) VALUES (?, ?, ?, ?)",
                orderId, "Migration Test", new BigDecimal("10.00"), 1);
    }

    private void sendRequests(
            TestRestTemplate client,
            Long productId,
            AtomicBoolean running,
            AtomicInteger requestNumber,
            CountDownLatch requestsStarted,
            Queue<Throwable> errors) {
        boolean started = false;
        while (running.get()) {
            try {
                int number = requestNumber.incrementAndGet();
                CreateOrderRequest request = new CreateOrderRequest(
                        "Load User " + number, "Test Address", null, "regular", null,
                        List.of(new CreateOrderRequest.Item(productId, 1)));
                assertSuccessful(client.postForEntity("/orders", request, Map.class));
                assertSuccessful(client.getForEntity("/orders", List.class));
            } catch (Throwable error) {
                errors.add(error);
            } finally {
                if (!started) {
                    started = true;
                    requestsStarted.countDown();
                }
            }
        }
    }

    private void assertSuccessful(ResponseEntity<?> response) {
        if (response.getStatusCode().is5xxServerError()) {
            throw new AssertionError("HTTP " + response.getStatusCode().value());
        }
    }

    private void migrateContract(Queue<Throwable> errors) {
        try {
            Flyway.configure().dataSource(dataSource).target("4").load().migrate();
        } catch (Throwable error) {
            errors.add(error);
        }
    }
}
