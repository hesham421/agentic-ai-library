package io.agenticai.chk.service;

import io.agenticai.chk.contract.CheckEngine;
import io.agenticai.chk.contract.ConfirmedCheck;
import io.agenticai.chk.contract.StartedCheck;
import io.agenticai.chk.domain.ActiveCheckStatus;
import io.agenticai.chk.error.CheckEngineErrorCodes;
import io.agenticai.chk.error.CheckEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The {@link CheckEngine} INT is given — a NON-transactional facade, so that each operation's
 * READ_WRITE transaction ({@link CheckStartService}, {@link CheckConfirmationService}) has
 * committed when it returns, and only then is the pipeline submitted to the bounded executor
 * ("return-then-submit"; SVC-API startCheck step 6, confirmUploads step 4). The call returns at
 * once; the pipeline runs in the background (REQ-CHK-001).
 *
 * <p>The typed rejections of {@code chk.contract} pass through unchanged; any other failure of a
 * start or confirmation is the module's unexpected failure, {@code CHK-500}, with its cause.
 * Orchestration only.
 */
@Service
public class CheckEngineService implements CheckEngine {

    private static final Logger log = LoggerFactory.getLogger(CheckEngineService.class);

    private final CheckStartService starts;
    private final CheckConfirmationService confirmations;
    private final CheckPipeline pipeline;
    private final PipelineRunner runner;
    private final CheckEndingService ending;

    public CheckEngineService(CheckStartService starts,
                              CheckConfirmationService confirmations,
                              CheckPipeline pipeline,
                              PipelineRunner runner,
                              CheckEndingService ending) {
        this.starts = Objects.requireNonNull(starts, "starts");
        this.confirmations = Objects.requireNonNull(confirmations, "confirmations");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.ending = Objects.requireNonNull(ending, "ending");
    }

    @Override
    public StartedCheck startCheck(String serviceCode, String requestNumber, String employeeId) {
        StartedRun run = translated(() -> starts.start(serviceCode, requestNumber, employeeId));
        if (run.context() != null) {
            launch(run.checkId(), run.context());
        }
        return new StartedCheck(run.checkId(), run.status().storedValue());
    }

    @Override
    public ConfirmedCheck confirmUploads(Long checkId) {
        ConfirmedRun confirmed = translated(() -> confirmations.confirm(checkId));
        Optional<CheckContext> context;
        try {
            context = confirmations.resume(confirmed);
        } catch (RuntimeException failure) {
            // step 4 failed after the commit: the Check is RUNNING and must still end (REQ-CHK-010)
            log.error("CHK resume of Check {} failed", checkId, failure);
            PipelineFailure ended = PipelineFailure.unexpected();
            ending.fail(checkId, ended.reason(), ended.detail());
            context = Optional.empty();
        }
        context.ifPresent(resumed -> launch(checkId, resumed));
        return new ConfirmedCheck(checkId, ActiveCheckStatus.RUNNING.storedValue());
    }

    private void launch(Long checkId, CheckContext context) {
        try {
            runner.submit(checkId, () -> pipeline.run(context));
        } catch (TaskRejectedException rejected) {
            log.error("CHK pipeline of Check {} could not be submitted", checkId, rejected);
            context.clear();
            PipelineFailure ended = PipelineFailure.unexpected();
            ending.fail(checkId, ended.reason(), ended.detail());
        }
    }

    private static <T> T translated(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (CheckEngineException refusal) {
            throw refusal;
        } catch (RuntimeException failure) {
            throw new CheckEngineException(CheckEngineErrorCodes.UNEXPECTED_FAILURE, failure);
        }
    }
}
