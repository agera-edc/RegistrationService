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

public class EnrollmentUrlResolver {


    private final DidResolver resolver;

    public EnrollmentUrlResolver(DidResolver resolver) {
        this.resolver = resolver;
    }

    @NotNull
    public Optional<String> resolveUrl(String didURI) {
        Result<DidDocument> didDocument = resolver.resolve(didURI);
        Optional<String> enrollmentUrl = didDocument.getContent().getService().stream().filter(service -> service.getType().equals("EnrollmentUrl")).map(Service::getServiceEndpoint).findFirst();
        return enrollmentUrl;
    }

}
