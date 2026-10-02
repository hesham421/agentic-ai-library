package io.agenticai.chk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled-task processing for the deployable — the ONE {@code @EnableScheduling}
 * of the code base, owned by CHK because its deadline check ({@code DeadlineCheckService},
 * REQ-CHK-080) is the only scheduled task. Spring Boot's auto-configured task scheduler runs it.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class CheckSchedulingConfiguration {
}
