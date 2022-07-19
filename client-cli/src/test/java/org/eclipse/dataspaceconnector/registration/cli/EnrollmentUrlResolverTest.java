package org.eclipse.dataspaceconnector.registration.cli;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

class EnrollmentUrlResolverTest {

    public static final String SERVICE_TYPE = "EnrollmentUrl";

    DidResolver didResolver = mock(WebDidResolver.class);
    EnrollmentUrlResolver enrollmentUrlResolver = new EnrollmentUrlResolver(didResolver);

    String didURI = "did:web:didserver";

    @Test
    void resolve_url_success() throws IOException {

        String apiUrl = "http://enrollmentUrl/api";
        DidDocument didDocument = didDocument(apiUrl);
        when(didResolver.resolve(didURI)).thenReturn(Result.success(didDocument));

        Optional<String> resultApiUrl = enrollmentUrlResolver.resolveUrl(didURI);

        assertThat(resultApiUrl).isNotEmpty();
        assertThat(resultApiUrl.get()).isEqualTo(apiUrl);

    }

    @Test
    void resolve_url_failure() {

        DidDocument didDocument = DidDocument.Builder.newInstance().build();
        when(didResolver.resolve(didURI)).thenReturn(Result.success(didDocument));

        Optional<String> resultApiUrl = enrollmentUrlResolver.resolveUrl(didURI);

        assertThat(resultApiUrl).isEmpty();

    }

    private DidDocument didDocument(String apiUrl) {
        return DidDocument.Builder.newInstance().service(List.of(new Service("some-id", SERVICE_TYPE, apiUrl))).build();
    }

}