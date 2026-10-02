package io.agenticai.reg.adapter;

import io.agenticai.reg.config.ServiceRegistryProperties;
import io.agenticai.reg.port.Activation;
import io.agenticai.reg.port.ActivationSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The {@link ActivationSource} over {@code aias.registry.*} (ADR-REG-009): maps the bound
 * {@link ServiceRegistryProperties} to the port's {@link Activation} one to one — same order,
 * duplicates kept, nothing validated, nothing resolved. The credential reference is passed on
 * as the name it is; no secret store is consulted (REQ-REG-054).
 */
@Component
public class PropertiesActivationSource implements ActivationSource {

    private final ServiceRegistryProperties properties;

    public PropertiesActivationSource(ServiceRegistryProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public Activation activation() {
        List<Activation.ConnectionEntry> entries = new ArrayList<>(properties.connections().size());
        for (ServiceRegistryProperties.Connection declared : properties.connections()) {
            entries.add(toEntry(declared));
        }
        return new Activation(properties.environmentName(), entries);
    }

    private static Activation.ConnectionEntry toEntry(ServiceRegistryProperties.Connection declared) {
        if (declared == null) {
            return new Activation.ConnectionEntry(null, null, null, null, null, null, null, null);
        }
        return new Activation.ConnectionEntry(
                declared.name(),
                declared.type(),
                declared.endpoint(),
                declared.queryTool(),
                declared.dialect(),
                declared.credentialReference(),
                declared.readOnly(),
                declared.limitedToViews());
    }
}
