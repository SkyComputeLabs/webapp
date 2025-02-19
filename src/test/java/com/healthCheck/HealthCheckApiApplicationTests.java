package com.healthCheck;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

import com.healthCheck.repository.HealthCheckRepository;
import com.healthCheck.service.HealthCheckService;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class HealthCheckApiApplicationTests {
	
//	private static final String BASE_URL = "http://localhost:8080/healthz";
	@LocalServerPort
	private int port;
	
	private String getBaseUrl() {
		return "http://localhost:" + port + "/healthz";
	}
	
	@Mock
	private HealthCheckRepository healthCheckRepo;
	
	 @Mock
	    private HealthCheckService healthCheckService;
	
	 @BeforeEach
	    void setUp() {
	        MockitoAnnotations.openMocks(this);
	    }
	
	@Test
    public void testGetRequestReturns200() {
        given()
            .when().get(getBaseUrl())
            .then().statusCode(200)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0")
            .body(is(emptyString()));  // No response body expected
    }

    @Test
    public void testBadRequestWhenQueryParamIsPresent() {
        given()
            .queryParam("unexpected", "value") // Sending a query param
            .when().get(getBaseUrl())
            .then().statusCode(400)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0")
            .body(is(emptyString()));
    }

    @Test
    public void testBadRequestWhenBodyIsPresent() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"key\":\"value\"}") // Sending a body
            .when().get(getBaseUrl())
            .then().statusCode(400)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0")
            .body(is(emptyString()));
    }

    @Test
    public void testPostMethodReturns405() {
        given()
            .when().post(getBaseUrl())
            .then().statusCode(405)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0")
            .body(is(emptyString()));
    }

    @Test
    public void testPutMethodReturns405() {
        given()
            .when().put(getBaseUrl())
            .then().statusCode(405)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0")
            .body(is(emptyString()));
    }

    @Test
    public void testDeleteMethodReturns405() {
        given()
            .when().delete(getBaseUrl())
            .then().statusCode(405)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0")
            .body(is(emptyString()));
    }

    @Test
    public void testHeadMethodReturns405() {
        given()
            .when().head(getBaseUrl())
            .then().statusCode(405)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", is(emptyOrNullString()));
    }

    @Test
    public void testOptionsMethodReturns405() {
        given()
            .when().options(getBaseUrl())
            .then().statusCode(405)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Length", "0");
    }

//    @Test
//    public void testServiceUnavailableWhenDatabaseDown() throws Exception{
//        // Simulate database failure
//    	when(healthCheckRepo.findAll()).thenThrow(new RuntimeException("Database is down"));
//        given()
//            .when().get(getBaseUrl())
//            .then().statusCode(503)
//            .header("Cache-Control", "no-cache, no-store, must-revalidate")
//            .header("Pragma", "no-cache")
//            .header("X-Content-Type-Options", "nosniff")
//            .header("Content-Length", "0")
//            .body(is(emptyString()));
//        
//        verify(healthCheckRepo, times(1)).findAll();
//    }
}
