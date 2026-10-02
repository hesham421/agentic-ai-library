package io.agenticai.doc.adapter;

import io.agenticai.doc.config.DataClass;
import io.agenticai.doc.config.DocumentAccessProperties;
import io.agenticai.doc.config.DocumentReadingModelConfiguration;
import io.agenticai.doc.config.ModelTier;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.DocumentReadingModelPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@link DocumentReadingModelPort} over Spring AI (REQ-DOC-027, ADR-DOC-004): the only class
 * of DOC that calls a model, and it reaches it solely through the provider-neutral
 * {@link ChatModel} / {@link Prompt} / {@link Media} API — no provider option class, no
 * provider feature (REQ-DOC-032, G12). The model is the dedicated
 * {@code documentReadingModel} bean of {@link DocumentReadingModelConfiguration}, injected by
 * qualifier so that no other model — the comparison model above all — can ever be the one
 * called here (REQ-DOC-030, AIAS-9); it is taken as an {@link ObjectProvider} because the bean
 * exists only while a model is configured, and the service must start without one
 * (REQ-DOC-033).
 *
 * <p>Every call goes through the same steps, in this order:
 * <ol>
 *   <li>the free-tier gate (ADR-DOC-009): tier {@code FREE} and data class {@code REAL} → no
 *       call, MODEL_NOT_PERMITTED (REQ-DOC-058, REQ-DOC-059);</li>
 *   <li>no model bean, or no reading instruction → READING_FAILED (REQ-DOC-033);</li>
 *   <li>the Check's deadline already reached → OUT_OF_TIME, no call (REQ-DOC-040);</li>
 *   <li>one new {@link Prompt} of exactly two parts: a system message that is the configured
 *       instruction and nothing else (REQ-DOC-046), and a user message with no text whose single
 *       {@link Media} is the document (REQ-DOC-057). Document bytes reach the model only as that
 *       media — data, never instruction text (REQ-DOC-045, REQ-DOC-047, G7). The prompt sets no
 *       option and registers no tool: with none in the model's own options the call declares 0
 *       tools (REQ-DOC-048, G1); there is no advisor, memory or earlier content (G9);</li>
 *   <li>the synchronous call runs on its own virtual thread and is waited for only as long as
 *       the Check has left; past that it is cancelled — the thread interrupted — and the
 *       document is OUT_OF_TIME (REQ-DOC-040, G8);</li>
 *   <li>the model's output is returned as text and never interpreted; a response with no text
 *       is {@code Read("")} — whether an empty reading is a usable one is the caller's call.</li>
 * </ol>
 *
 * <p>Failure translation (REQ-DOC-029; A.4.9, E.1.5): any exception out of Spring AI or its
 * HTTP client becomes a recorded READING_FAILED outcome naming the exception's type and
 * message, logged at debug with the cause. Neither the document's bytes nor the model's output
 * is ever logged or put in a detail. Stateless: nothing is kept between calls (G9).
 */
@Component
public class SpringAiDocumentReadingAdapter implements DocumentReadingModelPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiDocumentReadingAdapter.class);

    /** The user message carries the document as media and no text of its own (REQ-DOC-046). */
    private static final String NO_USER_TEXT = "";

    private final DocumentAccessProperties documents;
    private final ObjectProvider<ChatModel> readingModel;

    public SpringAiDocumentReadingAdapter(
            DocumentAccessProperties documents,
            @Qualifier(DocumentReadingModelConfiguration.DOCUMENT_READING_MODEL)
            ObjectProvider<ChatModel> readingModel) {
        this.documents = Objects.requireNonNull(documents, "documents");
        this.readingModel = Objects.requireNonNull(readingModel, "readingModel");
    }

    @Override
    public ReadOutcome<String> read(byte[] content, String mediaType, Instant deadline) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(deadline, "deadline");
        MimeType mimeType = MimeType.valueOf(mediaType);
        DocumentAccessProperties.ReadingModel configuration = documents.readingModel();

        // 1. the free-tier gate: a real document never reaches a free-tier model (ADR-DOC-009)
        if (configuration.tier() == ModelTier.FREE && documents.dataClass() == DataClass.REAL) {
            return unreadable(UnreadableReason.MODEL_NOT_PERMITTED,
                    "the document-reading model is tier FREE and the environment's data class is REAL; "
                            + "the document was not sent (ADR-DOC-009)");
        }
        // 2. a model and its instruction must be configured (REQ-DOC-033, REQ-DOC-046)
        ChatModel model = readingModel.getIfAvailable();
        if (model == null) {
            return unreadable(UnreadableReason.READING_FAILED,
                    "no document-reading model is configured (aias.documents.reading-model)");
        }
        String instruction = configuration.instruction();
        if (instruction == null || instruction.isBlank()) {
            return unreadable(UnreadableReason.READING_FAILED,
                    "no reading instruction is configured (aias.documents.reading-model.instruction)");
        }
        // 3. the Check's remaining time bounds the call; none left → no call (REQ-DOC-040)
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            return unreadable(UnreadableReason.OUT_OF_TIME,
                    "the Check's timeout was reached before the document-reading step");
        }
        // 4. one prompt, two parts: the configured instruction and the document as media
        Media document = Media.builder()
                .mimeType(mimeType)
                .data(content)
                .name("document." + mimeType.getSubtype())
                .build();
        Prompt prompt = new Prompt(
                new SystemMessage(instruction),
                UserMessage.builder().text(NO_USER_TEXT).media(document).build());
        log.debug("DOC reading model: calling model \"{}\" with one {} document of {} byte(s), "
                + "{} ms left", configuration.model(), mimeType, content.length, remaining.toMillis());

        // 5. the call, bounded by the remaining time
        FutureTask<ChatResponse> call = new FutureTask<>(() -> model.call(prompt));
        Thread worker = Thread.ofVirtual().name("doc-reading-model").unstarted(call);
        worker.start();
        ChatResponse response;
        try {
            response = call.get(remaining.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            log.debug("DOC reading model: the Check's timeout was reached during the call; cancelled");
            return unreadable(UnreadableReason.OUT_OF_TIME,
                    "the Check's timeout was reached while the document was being read");
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            log.debug("DOC reading model: the calling thread was interrupted; call cancelled", e);
            return unreadable(UnreadableReason.READING_FAILED,
                    "the document-reading call was interrupted");
        } catch (ExecutionException e) {
            // translation only: the model's failure becomes the document's recorded outcome
            Throwable cause = e.getCause() == null ? e : e.getCause();
            log.debug("DOC reading model: the call failed: {}", describe(cause), cause);
            return unreadable(UnreadableReason.READING_FAILED,
                    "the document-reading call failed: " + describe(cause));
        }
        // 6. the output as text, never interpreted
        String text = textOf(response);
        log.debug("DOC reading model: {} character(s) returned", text.length());
        return new Read<>(text);
    }

    /** The first generation's text; {@code ""} when the response carries none. */
    private static String textOf(ChatResponse response) {
        if (response == null) {
            return "";
        }
        Generation generation = response.getResult();
        if (generation == null) {
            return "";
        }
        AssistantMessage output = generation.getOutput();
        String text = output == null ? null : output.getText();
        return text == null ? "" : text;
    }

    private static ReadOutcome<String> unreadable(UnreadableReason reason, String detail) {
        return new Unreadable<>(reason, detail);
    }

    /** The exception's type and message — never the document or the model's output. */
    private static String describe(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + message;
    }
}
