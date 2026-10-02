package io.agenticai.chk.adapter;

import io.agenticai.chk.port.ServiceAvailability;
import io.agenticai.reg.contract.ServiceRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * The {@link ServiceAvailability} over the Service Registry's published in-process interface
 * (REQ-CHK-005, REQ-CHK-007; CON-REG-001): the only CHK class that asks {@link ServiceRegistry}
 * whether a service may be used. One call to CON-REG-013 {@code isServiceAvailable(serviceCode)}
 * per lookup, with the service code passed exactly as received — REG canonicalises it itself
 * (ADR-REG-017) — and never hardcoded.
 *
 * <p>The answer is {@code false} for a code the registry does not hold or whose service is
 * withdrawn; REG throws nothing for it, so there is nothing to translate. RULE-CHK-001 then
 * refuses the start with CHK-422-SERVICE-NOT-AVAILABLE before anything is created. No foreign
 * key, nothing kept.
 *
 * <p>Stateless: no field but the registry, no cache (G9). The REG interface is a bean of the same
 * deployable, injected by type.
 */
@Component("chkRegServiceAdapter")
public class RegServiceAdapter implements ServiceAvailability {

    private final ServiceRegistry registry;

    public RegServiceAdapter(ServiceRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean isServiceAvailable(String serviceCode) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        return registry.isServiceAvailable(serviceCode);
    }
}
