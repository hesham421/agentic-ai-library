package io.agenticai.doc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Environment settings of Document Access (DOC), bound from {@code aias.documents.*}. They are
 * environment configuration, never part of the deployable, and are read only from here — never
 * from a service package, a request or a document (REQ-DOC-010).
 *
 * <p>Nothing is required at binding, so the record is not {@code @Validated}: an absent storage
 * root is a valid state in which every {@code path} document is UNREADABLE / OUTSIDE_STORAGE_ROOT
 * and the service still starts (REQ-DOC-011); an absent reading model is a valid state in which
 * every document that needs the reading step is UNREADABLE / READING_FAILED (REQ-DOC-033).
 * Blocking start-up for either would also stop the reading that needs neither (ADR-DOC-009).
 *
 * @param storageRoot  the allowed storage root of the environment (REQ-DOC-010); {@code null}
 *                     when not set (REQ-DOC-011)
 * @param dataClass    the data class of the environment's documents (ADR-DOC-009); default
 *                     {@link DataClass#REAL}
 * @param readingModel the document-reading model's own configuration, separate from the
 *                     comparison model's (REQ-DOC-030, REQ-DOC-031); never {@code null}
 */
@ConfigurationProperties(prefix = "aias.documents")
public record DocumentAccessProperties(
        Path storageRoot,
        DataClass dataClass,
        ReadingModel readingModel) {

    /** Default of {@code aias.documents.data-class} (ADR-DOC-009). */
    public static final DataClass DEFAULT_DATA_CLASS = DataClass.REAL;

    public DocumentAccessProperties {
        dataClass = dataClass == null ? DEFAULT_DATA_CLASS : dataClass;
        readingModel = readingModel == null ? new ReadingModel(null, null, null, null) : readingModel;
    }

    /**
     * The document-reading model's configuration, {@code aias.documents.reading-model.*}.
     * Changing these properties and restarting switches the model with no code change
     * (REQ-DOC-030, REQ-DOC-031).
     *
     * @param provider    the model provider; {@code null} when not set
     * @param model       the model name; {@code null} when not set — then every document that
     *                    needs the reading step is UNREADABLE / READING_FAILED (REQ-DOC-033)
     * @param tier        the model's tier (ADR-DOC-009); default {@link ModelTier#FREE}
     * @param instruction the fixed reading instruction — the only instruction the model is ever
     *                    given, beside the document itself (REQ-DOC-046); {@code null} when not set
     */
    public record ReadingModel(
            String provider,
            String model,
            ModelTier tier,
            String instruction) {

        /** Default of {@code aias.documents.reading-model.tier} (ADR-DOC-009). */
        public static final ModelTier DEFAULT_TIER = ModelTier.FREE;

        public ReadingModel {
            tier = tier == null ? DEFAULT_TIER : tier;
        }
    }
}
