package org.noear.solon.cloud.extend.kafka.service;

import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventService;

/**
 * @author noear
 * @since 1.2
 */
public class CloudEventServiceImp implements CloudEventService {
    public CloudEventServiceImp(String server){

    }

    @Override
    public boolean push(Event event) {
        return false;
    }

    @Override
    public void attention(EventLevel level, String queue, String topic, CloudEventHandler observer) {

    }

    public void subscribe(){

    }
}
