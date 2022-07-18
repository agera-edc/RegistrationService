package org.eclipse.dataspaceconnector.registration.cli;

import org.eclipse.dataspaceconnector.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(DidResolverRegistry.class)
public class DidResolverRegistryExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(DidResolverRegistry.class, new DidResolverRegistryImpl());
    }
}
