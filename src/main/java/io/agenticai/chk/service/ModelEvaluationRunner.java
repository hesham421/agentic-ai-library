package io.agenticai.chk.service;

import io.agenticai.chk.config.ComparisonModelProperties;
import io.agenticai.chk.config.DataClass;
import io.agenticai.chk.config.EnvironmentDataClass;
import io.agenticai.chk.domain.CheckFailureReason;
import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.ComparisonModelPort;
import io.agenticai.chk.port.ConnectionLookup;
import io.agenticai.chk.port.DocumentOutcome;
import io.agenticai.chk.port.DocumentPort;
import io.agenticai.chk.port.QueryResult;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.port.ServiceQuery;
import io.agenticai.chk.port.ServiceQueryPort;
import io.agenticai.platform.config.CheckLimitsProperties;
import io.agenticai.reg.contract.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The model-evaluation run (REQ-CHK-069 … REQ-CHK-071, ADR-CHK-012; profile test phase MODEL-EVAL,
 * AIAS-10) — active only under the Spring profile {@code model-eval}, run on every comparison
 * model change. Each request of the known-result set ({@link KnownResultRequest}, one
 * {@code *.json} file per request in {@code aias.check.model-eval.directory}, default
 * {@code model-eval/known-result-set}) runs through the same {@link CheckPipeline} with in-memory
 * test doubles of the query, document and ending ports — defined here and used nowhere else — and
 * the configured comparison model. The run report has one row per request {request, expected,
 * reached}; any difference reports the run failed and names the request (REQ-CHK-071).
 *
 * <p>It sends only synthetic data: it refuses to run unless the environment data class is
 * {@code SYNTHETIC}, and refuses a set file not marked synthetic. Nothing is stored. The report
 * is written to the service log (no other destination is stated).
 */
@Component
@Profile("model-eval")
public class ModelEvaluationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelEvaluationRunner.class);

    /** Employee identity given to the synthetic Checks; the run records no Employee Decision. */
    private static final String EVALUATION_EMPLOYEE = "model-eval";

    private static final String OVER_ROW_LIMIT = "CHK-DETAIL-QUERY-OVER-ROW-LIMIT";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ComparisonModelPort comparison;
    private final CheckLimitsProperties limits;
    private final ComparisonModelProperties comparisonModel;
    private final EnvironmentDataClass dataClass;
    private final Path directory;

    public ModelEvaluationRunner(ComparisonModelPort comparison,
                                 CheckLimitsProperties limits,
                                 ComparisonModelProperties comparisonModel,
                                 EnvironmentDataClass dataClass,
                                 @Value("${aias.check.model-eval.directory:model-eval/known-result-set}") String directory) {
        this.comparison = Objects.requireNonNull(comparison, "comparison");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.comparisonModel = Objects.requireNonNull(comparisonModel, "comparisonModel");
        this.dataClass = Objects.requireNonNull(dataClass, "dataClass");
        this.directory = Path.of(Objects.requireNonNull(directory, "directory"));
    }

    @Override
    public void run(ApplicationArguments arguments) {
        evaluate();
    }

    /** One complete evaluation run. */
    public Report evaluate() {
        if (dataClass.dataClass() != DataClass.SYNTHETIC) {
            throw new IllegalStateException("The model-evaluation run sends only synthetic data: "
                    + EnvironmentDataClass.PROPERTY + " must be SYNTHETIC");
        }
        List<Row> rows = new ArrayList<>();
        long checkId = 0;
        for (Path file : setFiles()) {
            KnownResultRequest request = read(file);
            rows.add(new Row(request.request(), request.expectedOverallStatus(), runOne(++checkId, request)));
        }
        Report report = new Report(model(), List.copyOf(rows));
        rows.forEach(row -> log.info("CHK model-eval request={} expected={} reached={}",
                row.request(), row.expected(), row.reached()));
        if (report.passed()) {
            log.info("CHK model-eval run PASSED: model={} requests={}", report.model(), rows.size());
        } else {
            log.error("CHK model-eval run FAILED: model={} mismatched requests={}", report.model(), report.mismatched());
        }
        return report;
    }

    private String runOne(long checkId, KnownResultRequest request) {
        ServicePackageSnapshot servicePackage = request.servicePackage().snapshot();
        Capture ending = new Capture();
        CheckPipeline pipeline = new CheckPipeline(new InMemoryQueries(request, limits.maxRows()),
                new NoConnections(), new InMemoryDocuments(request, servicePackage.fetchMode()),
                comparison, limits, comparisonModel, ending);
        OffsetDateTime startedAt = OffsetDateTime.now();
        pipeline.run(CheckContext.of(checkId, servicePackage, request.request(), EVALUATION_EMPLOYEE,
                startedAt, startedAt.plus(limits.timeout()).toInstant()));
        return ending.reached;
    }

    private List<Path> setFiles() {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("The known-result request set cannot be read from " + directory, e);
        }
    }

    private static KnownResultRequest read(Path file) {
        KnownResultRequest request;
        try {
            request = JSON.readValue(Files.readString(file), KnownResultRequest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("The known-result request " + file.getFileName() + " cannot be read", e);
        }
        if (!request.synthetic()) {
            throw new IllegalStateException("The known-result request " + file.getFileName()
                    + " is not marked synthetic; the model-evaluation run sends only synthetic data");
        }
        return request;
    }

    private String model() {
        return comparisonModel.model();
    }

    /** One row of the run report (REQ-CHK-070). */
    public record Row(String request, String expected, String reached) {

        boolean matches() {
            return Objects.equals(expected, reached);
        }
    }

    /** The run report: passed only when every request reached its expected Overall Status. */
    public record Report(String model, List<Row> rows) {

        public boolean passed() {
            return rows.stream().allMatch(Row::matches);
        }

        public List<String> mismatched() {
            return rows.stream().filter(row -> !row.matches()).map(Row::request).toList();
        }
    }

    /** In-memory query double: the request's synthetic rows, the platform row limit applied. */
    private record InMemoryQueries(KnownResultRequest request, int maxRows) implements ServiceQueryPort {

        @Override
        public QueryResult run(ServiceQuery query, ConnectionSettings connection, Instant deadline) {
            String unread = request.unreadQueries().get(query.queryName());
            if (unread != null) {
                return new QueryResult.NotRead(unread);
            }
            List<Map<String, Object>> rows = request.queryResults().getOrDefault(query.queryName(), List.of());
            if (rows.size() > maxRows) {
                return new QueryResult.NotRead(CheckEngineTexts.english(OVER_ROW_LIMIT, maxRows));
            }
            return new QueryResult.Rows(rows);
        }
    }

    /** No connection is looked up: the in-memory query double reads no host. */
    private static final class NoConnections implements ConnectionLookup {

        @Override
        public Optional<ConnectionSettings> find(String connectionName) {
            return Optional.empty();
        }
    }

    /** In-memory document double: the request's synthetic outcomes; the end notice is a no-op. */
    private record InMemoryDocuments(KnownResultRequest request, String sourceMode) implements DocumentPort {

        @Override
        public List<DocumentOutcome> fetch(Long checkId, String requestNumber, String serviceCode,
                                           int versionNumber, Instant deadline) {
            return request.documents().stream().map(document -> document.outcome(sourceMode)).toList();
        }

        @Override
        public void endCheck(Long checkId) {
            // nothing was uploaded for a synthetic Check
        }
    }

    /** In-memory result capture: the reached Overall Status, or the failure reason. */
    private static final class Capture implements PipelineEnding {

        private String reached;

        @Override
        public void complete(Long checkId, CheckReport report) {
            reached = report.overallStatus().storedValue();
        }

        @Override
        public void fail(Long checkId, CheckFailureReason reason, String detail) {
            reached = "FAILED " + reason.storedValue();
        }
    }
}
