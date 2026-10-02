package io.agenticai.chk.service;

import io.agenticai.platform.config.CheckLimitsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * The bounded background executor of the Check pipelines (REQ-CHK-001, REQ-CHK-003): exactly
 * {@code aias.check.pipeline-threads} threads; further pipelines queue. The executor is owned
 * here, not exposed as a bean, so Spring Boot's own application task executor stays as
 * auto-configured.
 *
 * <p>It keeps the {@link Future} of each running pipeline by Check identifier so that the deadline
 * check can stop a timed-out pipeline ({@link #cancel}, REQ-CHK-051). This map holds identifiers
 * and futures ONLY — no query result, document content or model output (guardrail G9: it is not
 * Check data); an entry is removed when its pipeline ends or is cancelled.
 */
@Component
public class PipelineRunner implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final ThreadPoolTaskExecutor executor;
    private final ConcurrentMap<Long, Future<?>> running = new ConcurrentHashMap<>();

    public PipelineRunner(CheckLimitsProperties limits) {
        int threads = Objects.requireNonNull(limits, "limits").pipelineThreads();
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(threads);
        pool.setMaxPoolSize(threads);
        pool.setThreadNamePrefix("chk-pipeline-");
        pool.initialize();
        this.executor = pool;
    }

    /**
     * Runs the pipeline of a Check in the background.
     *
     * @throws org.springframework.core.task.TaskRejectedException the executor is shut down
     */
    void submit(Long checkId, Runnable pipeline) {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(pipeline, "pipeline");
        Runnable logged = () -> {
            try {
                pipeline.run();
            } catch (RuntimeException unhandled) {
                // the pipeline could not even hand its failure over; the Active Check stays and the
                // deadline check ends the Check TIMED_OUT (REQ-CHK-080) — nothing is lost silently
                log.error("CHK pipeline of Check {} stopped with an unhandled failure", checkId, unhandled);
            }
        };
        FutureTask<Void> task = new FutureTask<>(logged, null) {
            @Override
            protected void done() {
                running.remove(checkId, this);
            }
        };
        running.put(checkId, task);
        try {
            executor.execute(task);
        } catch (RuntimeException rejected) {
            running.remove(checkId, task);
            throw rejected;
        }
    }

    /** Stops the running pipeline of a Check, interrupting it (REQ-CHK-051); no-op when none runs. */
    void cancel(Long checkId) {
        Future<?> future = running.remove(checkId);
        if (future != null) {
            future.cancel(true);
            log.debug("CHK pipeline cancelled checkId={}", checkId);
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
