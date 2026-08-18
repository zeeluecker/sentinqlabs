package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/shopping")
public class ShoppingController {

    private final ShoppingOrchestrationService orchestrationService;

    public ShoppingController(
            ShoppingOrchestrationService orchestrationService
    ) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/orchestrate")
    public ShoppingOrchestrationResult orchestrate(
            @RequestBody ShoppingGoalRequest request
    ) {
        return orchestrationService.orchestrate(request);
    }

    @PostMapping(
            value = "/orchestrate-stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter orchestrateStream(
            @RequestBody ShoppingGoalRequest request
    ) {
        SseEmitter emitter =
                new SseEmitter(14 * 60 * 1000L);

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.submit(() -> {

            ScheduledExecutorService heartbeat =
                    Executors.newSingleThreadScheduledExecutor();

            try {
                /*
                 * Send data immediately so Railway does not
                 * consider the connection idle.
                 */
                emitter.send(
                        SseEmitter.event()
                                .name("status")
                                .data("Orchestration started")
                );

                /*
                 * Keep the Railway connection alive while
                 * Trust Maps are running.
                 */
                heartbeat.scheduleAtFixedRate(
                        () -> {
                            try {
                                emitter.send(
                                        SseEmitter.event()
                                                .name("heartbeat")
                                                .data("working")
                                );
                            } catch (Exception ignored) {
                                // Main orchestration thread will
                                // handle completion / failure.
                            }
                        },
                        20,
                        20,
                        TimeUnit.SECONDS
                );

                /*
                 * Existing orchestration remains unchanged.
                 */
                ShoppingOrchestrationResult result =
                        orchestrationService.orchestrate(
                                request
                        );

                emitter.send(
                        SseEmitter.event()
                                .name("complete")
                                .data(result)
                );

                emitter.complete();

            } catch (Exception e) {

                emitter.completeWithError(e);

            } finally {

                heartbeat.shutdownNow();
                executor.shutdown();
            }
        });

        return emitter;
    }
}