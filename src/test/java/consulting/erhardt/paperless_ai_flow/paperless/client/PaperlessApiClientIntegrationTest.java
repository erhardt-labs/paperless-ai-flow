package consulting.erhardt.paperless_ai_flow.paperless.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class PaperlessApiClientIntegrationTest {
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8089))
            .build();
    
    private PaperlessApiClient paperlessApiClient;
    
    @BeforeEach
    void setUp() {
        var config = PipelineConfiguration.builder()
                .api(PipelineConfiguration.ApiConfiguration.builder()
                        .baseUrl("http://localhost:8089")
                        .token("test-token")
                        .build())
                .build();
        
        var webClient = WebClient.builder()
                .baseUrl(config.getApi().getBaseUrl())
                .build();
        
        paperlessApiClient = new PaperlessApiClient(webClient, config);
    }
    
    @Test
    void getCorrespondents_shouldReturnCorrespondents() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/correspondents/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "count": 2,
                                  "next": null,
                                  "previous": null,
                                  "results": [
                                    {
                                      "id": 1,
                                      "name": "Company ABC",
                                      "is_insensitive": false,
                                      "match": "Company ABC",
                                      "matching_algorithm": 1,
                                      "document_count": 5,
                                      "last_correspondence": "2023-12-01"
                                    },
                                    {
                                      "id": 2,
                                      "name": "Supplier XYZ", 
                                      "is_insensitive": true,
                                      "match": "XYZ",
                                      "matching_algorithm": 2,
                                      "document_count": 3,
                                      "last_correspondence": "2023-11-15"
                                    }
                                  ]
                                }
                                """)));
        
        // When & Then
        StepVerifier.create(paperlessApiClient.getCorrespondents())
                .expectNextMatches(correspondent -> 
                        correspondent.getId().equals(1L) && 
                        "Company ABC".equals(correspondent.getName()) &&
                        !correspondent.getIsInsensitive() &&
                        correspondent.getDocumentCount().equals(5))
                .expectNextMatches(correspondent -> 
                        correspondent.getId().equals(2L) && 
                        "Supplier XYZ".equals(correspondent.getName()) &&
                        correspondent.getIsInsensitive() &&
                        correspondent.getDocumentCount().equals(3))
                .verifyComplete();
    }
    
    @Test
    void getCustomFields_shouldReturnCustomFields() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/custom_fields/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "count": 2,
                                  "next": null,
                                  "previous": null,
                                  "results": [
                                    {
                                      "id": 1,
                                      "name": "Amount",
                                      "data_type": "float",
                                      "extra_data": "",
                                      "created": "2023-01-01T00:00:00Z",
                                      "modified": "2023-01-01T00:00:00Z"
                                    },
                                    {
                                      "id": 2,
                                      "name": "Invoice Date",
                                      "data_type": "date",
                                      "extra_data": "",
                                      "created": "2023-01-02T00:00:00Z",
                                      "modified": "2023-01-02T00:00:00Z"
                                    }
                                  ]
                                }
                                """)));
        
        // When & Then
        StepVerifier.create(paperlessApiClient.getCustomFields())
                .expectNextMatches(customField -> 
                        customField.getId().equals(1L) && 
                        "Amount".equals(customField.getName()) &&
                        "float".equals(customField.getDataType()))
                .expectNextMatches(customField -> 
                        customField.getId().equals(2L) && 
                        "Invoice Date".equals(customField.getName()) &&
                        "date".equals(customField.getDataType()))
                .verifyComplete();
    }
    
    @Test
    void getCorrespondents_shouldHandleEmptyResponse() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/correspondents/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "count": 0,
                                  "next": null,
                                  "previous": null,
                                  "results": []
                                }
                                """)));
        
        // When & Then
        StepVerifier.create(paperlessApiClient.getCorrespondents())
                .verifyComplete();
    }
    
    @Test
    void getCustomFields_shouldHandleEmptyResponse() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/custom_fields/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "count": 0,
                                  "next": null,
                                  "previous": null,
                                  "results": []
                                }
                                """)));
        
        // When & Then
        StepVerifier.create(paperlessApiClient.getCustomFields())
                .verifyComplete();
    }
    
    @Test
    void getCorrespondents_shouldHandleApiError() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/correspondents/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
        
        // When & Then
        StepVerifier.create(paperlessApiClient.getCorrespondents())
                .expectError()
                .verify();
    }
    
    @Test
    void getCustomFields_shouldHandleApiError() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/custom_fields/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Unauthorized")));
        
        // When & Then
        StepVerifier.create(paperlessApiClient.getCustomFields())
                .expectError()
                .verify();
    }
}
