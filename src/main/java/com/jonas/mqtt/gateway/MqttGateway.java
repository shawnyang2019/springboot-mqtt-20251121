package com.jonas.mqtt.gateway;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/**
 * 发送消息接口
 * @MessagingGateway: 这是 Spring Integration 的核心注解之一，用于声明一个消息网关。
 * 一个 “网关” 就像一个门面（Facade），它隐藏了 Spring Integration 内部的通道、适配器等复杂组件，给开发者提供了一个纯净的、类似调用本地方法的体验来发送消息。
 * 当你的代码调用这个接口的方法时，Spring Integration 会自动将你的调用转换为一个 Spring Message 对象，并发送到指定的通道。
 * defaultRequestChannel = "mqttOutputChannel":
 * 这个属性指定了默认的请求通道。当你调用这个网关接口中没有明确指定通道的方法时，生成的消息将被发送到这个通道。
 *
 * 这个通道接着会把消息传递给与之关联的 mqttOutputHandler (一个 MqttPahoMessageHandler)，最终由它将消息发送到 MQTT 服务器。
 * @author shenjy 2019/06/10
 */
@MessagingGateway(defaultRequestChannel = "mqttOutputChannel")
public interface MqttGateway {

    /**
     * 向默认topic发送消息
     *
     * @param data 数据
     */
    void sendToMqtt(String data);

    /**
     * 指定 topic 进行消息发送
     *
     * @param data  数据
     * @param topic 主题
     */
    void sendToMqtt(String data, @Header(MqttHeaders.TOPIC) String topic);

    /**
     * 指定 topic 和 qos 进行消息发送
     *
     * @param data  数据
     * @param topic 主题
     * @param qos   服务质量
     */
    void sendToMqtt(String data, @Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) int qos);
}
