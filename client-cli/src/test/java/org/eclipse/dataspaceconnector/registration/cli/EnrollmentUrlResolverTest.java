package org.eclipse.dataspaceconnector.registration.cli;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;

class EnrollmentUrlResolverTest {

    public static final String SERVICE_TYPE = "EnrollmentUrl";

    DidResolver didResolver = mock(WebDidResolver.class);
    EnrollmentUrlResolver enrollmentUrlResolver = new EnrollmentUrlResolver(didResolver);

    String did = "did:web:didserver";

    @Test
    void resolveUrl_success() {

        String apiUrl = "http://enrollmentUrl/api";
        DidDocument didDocument = didDocument(apiUrl);
        when(didResolver.resolve(did)).thenReturn(Result.success(didDocument));

        Optional<String> resultApiUrl = enrollmentUrlResolver.resolveUrl(did);

        assertThat(resultApiUrl).isNotEmpty();
        assertThat(resultApiUrl.get()).isEqualTo(apiUrl);

    }

    @Test
    void resolveUrl_noEnrollmentUrl() {

        DidDocument didDocument = DidDocument.Builder.newInstance().build();
        when(didResolver.resolve(did)).thenReturn(Result.success(didDocument));

        Optional<String> resultApiUrl = enrollmentUrlResolver.resolveUrl(did);

        assertThat(resultApiUrl).isEmpty();

    }

    @Test
    void resolveUrl_failureToGetDid() {

        when(didResolver.resolve(did)).thenReturn(Result.failure("Failure"));

        assertThatThrownBy(() -> enrollmentUrlResolver.resolveUrl(did)).isInstanceOf(CliException.class);
    }

    private DidDocument didDocument(String apiUrl) {
        return DidDocument.Builder.newInstance().service(List.of(new Service("some-id", SERVICE_TYPE, apiUrl))).build();
    }

}