package com.jonas.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttSendConfiguration {

    @Autowired
    private MqttProperties mqttProp;

    @Autowired
    private MqttPahoClientFactory mqttClientFactory;
    /**
     * 这部分定义了消息发送流,消息发送到Mqtt服务器。
     * mqttOutputChannel():
     * 创建一个 DirectChannel，这是一个简单的消息通道，它将消息直接发送给一个订阅者（在这个场景下，就是 mqttOutputHandler）。它是同步的，意味着发送者会阻塞直到消息被处理。
     * 这个通道的作用就像一个 “邮筒”，应用程序的其他部分（如 Service）可以将待发送的 MQTT 消息投递到这个邮筒里。
     * mqttOutputHandler():
     * @ServiceActivator(inputChannel = "mqttOutputChannel"): 这是一个关键的 Spring Integration 注解。它将这个 MessageHandler Bean 注册为 mqttOutputChannel 通道的订阅者。任何发送到 mqttOutputChannel 的消息都会被这个处理器接收并处理。
     * MqttPahoMessageHandler: 这是 Spring Integration 提供的 MQTT 出站适配器。它负责将接收到的 Spring Message 对象转换为 MQTT 消息，并通过 Paho 客户端发送到 MQTT 服务器。
     * 构造函数参数:
     * mqttProp.getClientId(): 客户端 ID。
     * mqttClientFactory(): 它引用了我们之前定义的客户端工厂，以便获取连接配置。
     * handler.setAsync(true): 设置为异步发送。这意味着 handleMessage 方法会立即返回，而不会等待 MQTT 服务器的确认，提高了发送吞吐量，但牺牲了同步确认的能力。如果需要确保消息一定送达，可能需要设置为 false 并处理可能的异常。
     * handler.setDefaultTopic(...): 设置一个默认的发送主题。如果发送消息时没有在 MessageHeaders 中指定 mqtt_topic，则会使用这个默认主题。
     *
     */

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutputHandler() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(mqttProp.getClientId(), mqttClientFactory);
        // 如果设置成true，即异步，发送消息时将不会阻塞。
        handler.setAsync(true);
//        handler.setDefaultQos(2);
        handler.setDefaultTopic(mqttProp.getDefaultTopic());
        return handler;
    }

}
