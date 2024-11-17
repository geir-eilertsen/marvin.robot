package com.assetvisor.marvin.interaction.web.adapters;

import com.assetvisor.marvin.robot.domain.ports.ForTellingHumans;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component("web")
public class ForTellingHumansSseEmittingAdapter implements ForTellingHumans {

    private final Log LOG = LogFactory.getLog(getClass());

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        LOG.info("Added emitter: " + emitter);
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            LOG.info("Removed emitter: " + emitter);
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            LOG.info("Removed emitter: " + emitter);
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            LOG.error("Error in SSE connection: " + e.getMessage());
        });
    }


    @Override
    public void tell(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(message);
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
}