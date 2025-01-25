package com.mawippel.producers;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("disable-producer")
public class NoOpEventProducer implements EventProducer {

    @Override
    public void sendUpstreamEvent(UpstreamEventMessage message) {
        throw new RuntimeException("The wcs flow is not active in this environment because Kafka is not present.");
    }
}
