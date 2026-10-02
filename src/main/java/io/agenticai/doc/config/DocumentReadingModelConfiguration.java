package io.agenticai.doc.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * The document-reading model's own {@link ChatModel} bean, {@value #DOCUMENT_READING_MODEL}
 * (REQ-DOC-030, AIAS-9): built from {@code aias.documents.reading-model.*} and nothing of any
 * other model's configuration. The comparison model (CHK's) is a different bean and is never
 * this one; the Spring AI starter's own default chat model is switched off
 * ({@code spring.ai.model.chat=none}) so that no unqualified {@code ChatModel} exists to be
 * injected by mistake.
 *
 * <p><b>The single provider-binding seam (REQ-DOC-032, domain-profile G12).</b> This class is the
 * only place in the deployable that names a Spring AI provider: the provider's model and option
 * classes are used here, at construction time, to turn configuration into a {@code ChatModel};
 * everything else — the adapter included — sees the provider-neutral {@code ChatModel} /
 * {@code Prompt} / {@code Media} API and sets no option of its own. v1 supports one provider,
 * {@code openai}, through Spring AI's OpenAI-compatible module; a local or free-tier gateway
 * that speaks the same API is reached by its base URL. The go-live provider is decided later
 * (domain-profile D3) and switches by configuration alone (REQ-DOC-031): naming another model
 * is a property change; another provider is one more branch of this class.
 *
 * <p><b>Configuration.</b> Of the {@code aias.documents.reading-model.*} keys the model name
 * goes into the options (the only generation setting carried), the instruction and the tier
 * are the adapter's. The plan states no connection settings, so endpoint and credential are
 * read from the provider's standard Spring AI keys — recorded as an API-document gap:
 * <ul>
 *   <li>{@code spring.ai.openai.base-url} — absent: Spring AI's default endpoint (or its
 *       {@code OPENAI_BASE_URL} environment fallback);</li>
 *   <li>{@code spring.ai.openai.api-key} — absent: a client that sends no credential, which is
 *       what a local gateway needs; a remote endpoint then answers 401, recorded as
 *       READING_FAILED, and start-up is never blocked on a secret (REQ-DOC-033).</li>
 * </ul>
 * The bean exists only while {@code provider} is {@code openai} (case-insensitive) and
 * {@code model} is set ({@link ReadingModelConfigured}); any other provider, or no model, means
 * no document-reading model is configured — the adapter then records READING_FAILED
 * (REQ-DOC-033) and the service starts regardless. Spring AI's per-request HTTP timeout and
 * retry stay at the module's defaults; the Check's remaining time bounds every call from the
 * adapter's side (REQ-DOC-040).
 */
@Configuration(proxyBeanMethods = false)
@Conditional(DocumentReadingModelConfiguration.ReadingModelConfigured.class)
public class DocumentReadingModelConfiguration {

    /** The bean name and qualifier of the document-reading model (REQ-DOC-030). */
    public static final String DOCUMENT_READING_MODEL = "documentReadingModel";

    /** The one provider v1 supports; compared case-insensitively. */
    public static final String SUPPORTED_PROVIDER = "openai";

    static final String PROVIDER_KEY = "aias.documents.reading-model.provider";
    static final String MODEL_KEY = "aias.documents.reading-model.model";
    static final String BASE_URL_KEY = "spring.ai.openai.base-url";
    static final String API_KEY_KEY = "spring.ai.openai.api-key";

    @Bean(DOCUMENT_READING_MODEL)
    @Qualifier(DOCUMENT_READING_MODEL)
    ChatModel documentReadingModel(DocumentAccessProperties documents, Environment environment) {
        Objects.requireNonNull(documents, "documents");
        Objects.requireNonNull(environment, "environment");
        String model = documents.readingModel().model();
        // the condition guarantees both; restated so the bean can never be built half-configured
        if (!SUPPORTED_PROVIDER.equalsIgnoreCase(documents.readingModel().provider())
                || !StringUtils.hasText(model)) {
            throw new IllegalStateException(
                    "The document-reading model bean needs " + PROVIDER_KEY + "=" + SUPPORTED_PROVIDER
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
        // Spring AI's defaults and, with no tool callback in its options, declares 0 tools.
        return OpenAiChatModel.builder().options(options).build();
    }

    /**
     * Holds while a document-reading model is configured: the provider is the supported one and
     * a model is named. Read from the {@link Environment} because the condition runs before
     * {@link DocumentAccessProperties} is bound.
     */
    static final class ReadingModelConfigured implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return SUPPORTED_PROVIDER.equalsIgnoreCase(trimmed(environment.getProperty(PROVIDER_KEY)))
                    && StringUtils.hasText(environment.getProperty(MODEL_KEY));
        }

        private static String trimmed(String value) {
            return value == null ? null : value.trim();
        }
    }
}
