package com.jonas.test;

import com.jonas.Application;
import com.jonas.controller.MqttController;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

@SpringBootTest(classes= Application.class)
public class MqttMessageSenderTest
{
    @Autowired
    private MqttController mqttController;

    @Test
    public void sendToMsg(){
        mqttController.sendMsg("aaa");
    }
}
