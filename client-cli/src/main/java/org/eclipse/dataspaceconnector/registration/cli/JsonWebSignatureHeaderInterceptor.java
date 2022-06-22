package org.eclipse.dataspaceconnector.registration.cli;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.time.Clock;
import java.util.function.Consumer;

public class JsonWebSignatureHeaderInterceptor implements Consumer<HttpRequest.Builder> {

    private final RegistrationServiceCli registrationServiceCli;
    private final PrivateKeyWrapper privateKey;

    public JsonWebSignatureHeaderInterceptor(RegistrationServiceCli registrationServiceCli) {
        this.registrationServiceCli = registrationServiceCli;
        try {
            var ecKey = (ECKey) ECKey.parseFromPEMEncodedObjects(Files.readString(registrationServiceCli.privateKey));
            privateKey = new EcPrivateKeyWrapper(ecKey);
        } catch (IOException e) {
            throw new EdcException(e);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void accept(HttpRequest.Builder b) {
        var token = VerifiableCredentialFactory.create(
                privateKey,
                registrationServiceCli.clientDid,
                registrationServiceCli.service,
                Clock.systemUTC()).serialize();
        b.header("Authorization", String.format("Bearer %s", token));
    }
}