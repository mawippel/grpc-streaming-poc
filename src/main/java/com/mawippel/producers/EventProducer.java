package com.mawippel.producers;


public interface EventProducer {

    void sendUpstreamEvent(UpstreamEventMessage message);
}
