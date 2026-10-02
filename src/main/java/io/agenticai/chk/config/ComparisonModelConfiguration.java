package io.agenticai.chk.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * The comparison model's own {@link ChatModel} bean, {@value #COMPARISON_MODEL} (REQ-CHK-068,
 * AIAS-9): built from {@code aias.check.comparison-model.*} ({@link ComparisonModelProperties})
 * and nothing of any other model's configuration. The document-reading model of Document Access
 * is a different bean and is never injected into CHK; the Spring AI starter's own default chat
 * model is switched off ({@code spring.ai.model.chat=none}) so no unqualified {@code ChatModel}
 * exists to be injected by mistake.
 *
 * <p><b>The single provider-binding seam of CHK (REQ-CHK-067, domain-profile G12).</b> This class
 * is the only place in CHK that names a Spring AI provider: the provider's model and option
 * classes turn configuration into a {@code ChatModel} here, at construction time; the adapter
 * sees only the provider-neutral {@code ChatModel} / {@code Prompt} API and sets no option of its
 * own. v1 supports one provider, {@code openai}, through Spring AI's OpenAI-compatible module; a
 * local or free-tier gateway that speaks the same API is reached by its base URL. Another model
 * is a property change; another provider is one more branch of this class — no other CHK source
 * changes (AC-CHK-070).
 *
 * <p><b>Configuration.</b> Of the {@code aias.check.comparison-model.*} keys the model name goes
 * into the options (the only generation setting carried); the tier is the adapter's gate. The
 * plan states no connection settings, so — as for the document-reading model — the endpoint and
 * credential are read from the provider's standard Spring AI keys:
 * <ul>
 *   <li>{@code spring.ai.openai.base-url} — absent: Spring AI's default endpoint (or its
 *       {@code OPENAI_BASE_URL} environment fallback);</li>
 *   <li>{@code spring.ai.openai.api-key} — absent: a client that sends no credential, which is
 *       what a local gateway needs; a remote endpoint then answers 401, which the Check records
 *       as MODEL_UNAVAILABLE, and start-up is never blocked on a secret.</li>
 * </ul>
 * The bean exists only while {@code provider} is {@code openai} (case-insensitive) and
 * {@code model} is set ({@link ComparisonModelConfigured}); otherwise no comparison model is
 * configured, the service starts regardless, and a Check reaching the comparison step ends
 * FAILED / MODEL_UNAVAILABLE (ADR-CHK-005). Spring AI's per-request HTTP timeout and retry stay at
 * the module's defaults; the Check's remaining time bounds every call from the adapter's side
 * (REQ-CHK-051).
 */
@Configuration(proxyBeanMethods = false)
@Conditional(ComparisonModelConfiguration.ComparisonModelConfigured.class)
public class ComparisonModelConfiguration {

    /** The bean name and qualifier of the comparison model (REQ-CHK-068). */
    public static final String COMPARISON_MODEL = "comparisonModel";

    /** The one provider v1 supports; compared case-insensitively. */
    public static final String SUPPORTED_PROVIDER = "openai";

    static final String PROVIDER_KEY = "aias.check.comparison-model.provider";
    static final String MODEL_KEY = "aias.check.comparison-model.model";
    static final String BASE_URL_KEY = "spring.ai.openai.base-url";
    static final String API_KEY_KEY = "spring.ai.openai.api-key";

    @Bean(COMPARISON_MODEL)
    @Qualifier(COMPARISON_MODEL)
    ChatModel comparisonModel(ComparisonModelProperties comparison, Environment environment) {
        Objects.requireNonNull(comparison, "comparison");
        Objects.requireNonNull(environment, "environment");
        String model = comparison.model();
        // the condition guarantees both; restated so the bean can never be built half-configured
        if (!SUPPORTED_PROVIDER.equalsIgnoreCase(trimmed(comparison.provider()))
                || !StringUtils.hasText(model)) {
            throw new IllegalStateException(
                    "The comparison model bean needs " + PROVIDER_KEY + "=" + SUPPORTED_PROVIDER
                            + " and a " + MODEL_KEY);
        }
        // Connection settings of the provider; "" asks Spring AI for a client without a credential.
        String baseUrl = environment.getProperty(BASE_URL_KEY);
        String apiKey = environment.getProperty(API_KEY_KEY, "");
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model.trim())
                .baseUrl(StringUtils.hasText(baseUrl) ? baseUrl.trim() : null)
                .apiKey(apiKey.trim())
                .build();
        // No tool-calling manager, observation or HTTP customiser is handed in: the model keeps
        // Spring AI's defaults and, with no tool callback in its options, declares 0 tools
        // (REQ-CHK-030).
        return OpenAiChatModel.builder().options(options).build();
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Holds while a comparison model is configured: the provider is the supported one and a model
     * is named. Read from the {@link Environment} because the condition runs before
     * {@link ComparisonModelProperties} is bound.
     */
    static final class ComparisonModelConfigured implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return SUPPORTED_PROVIDER.equalsIgnoreCase(trimmed(environment.getProperty(PROVIDER_KEY)))
                    && StringUtils.hasText(environment.getProperty(MODEL_KEY));
        }
    }
}
