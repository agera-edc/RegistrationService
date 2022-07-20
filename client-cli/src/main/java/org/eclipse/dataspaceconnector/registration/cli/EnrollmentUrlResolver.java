package org.eclipse.dataspaceconnector.registration.cli;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.web.resolution.WebDidResolver;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves the enrollment url from the DID.
 */
public class EnrollmentUrlResolver {

    public static final String ENROLLMENT_URL = "EnrollmentUrl";

    /**
     * Resolves the DID document from did:web.
     */
    private final DidResolver resolver;

    public EnrollmentUrlResolver(DidResolver resolver) {
        this.resolver = resolver;
    }

    @NotNull
    public Optional<String> resolveUrl(String did) {
        Result<DidDocument> didDocument = resolver.resolve(did);
        if (didDocument.failed()) {
            throw new CliException("Error resolving the DID " + did);
        }
        return didDocument.getContent().getService()
                .stream()
                .filter(service -> service.getType().equals(ENROLLMENT_URL))
                .map(Service::getServiceEndpoint).findFirst();
    }

}
