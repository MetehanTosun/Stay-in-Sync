package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingException;
import de.unistuttgart.stayinsync.transport.dto.*;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static de.unistuttgart.stayinsync.transport.dto.ParamType.QUERY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RequestBuilder contains a single public method called 'buildRequest', that builds a HttpRequest with the information from RequestBuildingDetails.
 * 'buildRequest' is tested in this TestClass.
 */
@ExtendWith(MockitoExtension.class)
public class RequestBuilderTest {

    @Mock
    private Vertx vertx;

    @Mock
    private WebClient webClient;

    @Mock
    private HttpRequest<Buffer> httpRequest;

    private RequestBuilder requestBuilder;

    private MockedStatic<WebClient> webClientMockedStatic;

    @BeforeEach
    void setUp() {
        webClientMockedStatic = mockStatic(WebClient.class);
        webClientMockedStatic.when(() -> WebClient.create(eq(vertx), any())).thenReturn(webClient);

        requestBuilder = new RequestBuilder(vertx);
    }

    @AfterEach
    void tearDown() {
        if (webClientMockedStatic != null) {
            webClientMockedStatic.close();
        }
    }

    @Test
    @DisplayName("Tests if all needed methods are called on WebClient to build a Request if exactly the information of the RequestBuildingDetails during 'buildRequest'.")
    void testBuildRequestSuccessful(){
        // Arrange
        RequestBuildingDetails requestBuildingDetails = createRequestBuildingDetailsWithNullFieldForSpecificValue(0);

        when(webClient.getAbs(anyString())).thenReturn(httpRequest);
        when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);

        // Act
        try {
            requestBuilder.buildRequest(requestBuildingDetails);
        } catch(Exception e){
            fail("Exception was thrown during buildRequest call", e);
        }

        // Assert
        verify(webClient).getAbs("https://api.mysystem.com/v1/projects");
        verify(httpRequest).putHeader("Accept", "/application\\json");
        verify(httpRequest).addQueryParam("high", "value");
        verify(httpRequest).addQueryParam("open", "otherValue");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Tests if appropriate exceptions are thrown when individual fields are null")
    void testIfNullFieldsLeadToExceptions(int nullFieldIndex){
        // Arrange
        RequestBuildingDetails invalidRequestBuildingDetails = createRequestBuildingDetailsWithNullFieldForSpecificValue(nullFieldIndex);
        // Act + Assert
        assertThrows(RequestBuildingException.class, () -> requestBuilder.buildRequest(invalidRequestBuildingDetails));
    }

    private RequestBuildingDetails createRequestBuildingDetailsWithNullFieldForSpecificValue(final int nullFieldIndex) {
        String authType = "Bearer";
        String authToken = "token123";
        String systemName = "MySystem";
        String systemUrl = "https://api.mysystem.com\\";
        String systemType = "REST";
        String endpointPath = "/v1\\projects";
        String endpointMethod = "GET";
        String paramName1 = "high";
        String paramValue1 = "value";
        String paramName2 = "open";
        String paramValue2 = "otherValue";
        String headerName = "Accept";
        String headerValue = "/application\\json";
        ParamType paramType = QUERY;

        switch (nullFieldIndex) {
            case 0: break;
            case 1: authType = null; break;
            case 2: authToken = null; break;
            case 3: systemUrl = null; break;
            case 4: endpointPath = null; break;
            case 5: endpointMethod = null; break;
            case 6: paramType = null; break;
        }

        ApiAuthConfigurationMessageDTO authDetails = new ApiAuthConfigurationMessageDTO(authType, authToken);
        SourceSystemMessageDTO sourceSystem = new SourceSystemMessageDTO(systemName, systemUrl, systemType, authDetails);
        SourceSystemEndpointMessageDTO endpoint = new SourceSystemEndpointMessageDTO(endpointPath, endpointMethod);

        ApiRequestParameterMessageDTO param1 = new ApiRequestParameterMessageDTO(paramType, paramName1, paramValue1);
        ApiRequestParameterMessageDTO param2 = new ApiRequestParameterMessageDTO(paramType, paramName2, paramValue2);
        ApiRequestHeaderMessageDTO header1 = new ApiRequestHeaderMessageDTO(headerName, headerValue);

        return new RequestBuildingDetails(sourceSystem, endpoint, Set.of(param1, param2), Set.of(header1));
    }

}
