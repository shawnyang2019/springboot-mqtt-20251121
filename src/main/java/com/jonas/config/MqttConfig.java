package com.jonas.config;

import com.jonas.mqtt.handler.ReceiveMessageHandler;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.router.MessageRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * 【 enter the class description 】
 *
 * @author shenjy 2019/06/10
 */
@Configuration
/**
 * @IntegrationComponentScan 注解的作用是扫描带有 Spring Integration 注解（如 @MessagingGateway、@Transformer 等）的组件。
 * 如果你的 MqttGateway 接口所在的包（com.jonas.mqtt.gateway）是 MqttConfig 类所在包（com.jonas.config）的子包或同级包，那么现有的 @IntegrationComponentScan 注解就足够了。
 * 如果 MqttGateway 在一个完全不同的包路径下，你需要明确指定扫描路径：
 * */
@IntegrationComponentScan(basePackages = "com.jonas.mqtt.gateway")
public class MqttConfig {

    @Autowired
    private MqttProperties mqttProp;

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttProp.getUsername());
        options.setPassword(mqttProp.getPassword().toCharArray());
        options.setServerURIs(mqttProp.getHostUrl());
//        options.setCleanSession(true);
        return options;
    }

    /**
     * 创建MqttPahoClientFactory，设置MQTT Broker连接属性，如果使用SSL验证，也在这里设置。
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions());
        return factory;
    }
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
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(mqttProp.getClientId(), mqttClientFactory());
        // 如果设置成true，即异步，发送消息时将不会阻塞。
        handler.setAsync(true);
//        handler.setDefaultQos(2);
        handler.setDefaultTopic(mqttProp.getDefaultTopic());
        return handler;
    }


    /**
     * 这部分定义了消息接收流，从监听的topic中接收消息。
     * mqttInputChannel():
     * 同样创建了一个 DirectChannel，用于承载从 MQTT 服务器接收到的消息。
     * messageProducer():
     * 这个 Bean 是 Spring Integration 的 MQTT 入站适配器，它是一个 MessageProducer。它负责订阅 MQTT 主题，并在接收到消息时，将其转换为 Spring Message 对象，然后发送到指定的 outputChannel。
     * MqttPahoMessageDrivenChannelAdapter: 这是入站适配器的实现。
     * 构造函数参数:
     * mqttProp.getClientId() + "_input": 为这个消费者客户端指定一个唯一的 ID。通常在生产者 ID 后加一个后缀（如 _input）来区分。
     * mqttClientFactory(): 引用客户端工厂。
     * mqttProp.getSubscriptionTopic(): 指定要订阅的一个或多个 MQTT 主题。
     * adapter.setConverter(...): 设置一个消息转换器。DefaultPahoMessageConverter 会将 Paho 库的 MqttMessage 对象转换为 Spring 的 Message 对象，消息体（payload）默认为 byte[] 类型。你可以自定义转换器来处理更复杂的消息格式（如 JSON）。
     * adapter.setOutputChannel(mqttInputChannel()): 指定将接收到的消息发送到哪个通道。这里是 mqttInputChannel。
     * mqttInputHandler():
     * @ServiceActivator(inputChannel = "mqttInputChannel"): 将这个自定义的 MessageHandler 订阅到 mqttInputChannel。
     * return new ReceiveMessageHandler(): 这是你自己实现的业务逻辑处理器。它必须实现 Spring 的 MessageHandler 接口。当有消息到达 mqttInputChannel 时，Spring Integration 会自动调用它的 handleMessage(Message<?> message) 方法，你可以在这个方法中编写处理 MQTT 消息的具体业务逻辑（例如，解析消息、存入数据库、触发其他业务流程等）。
     *
     *
     * */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /** 配置client监听的topic 入站适配器 */
    @Bean
    public MessageProducer messageProducer() {
        MqttPahoMessageDrivenChannelAdapter adapter =
//                sub 的客户端id 在默认的client_id的基础上加上_input
                new MqttPahoMessageDrivenChannelAdapter(mqttProp.getClientId() + "_input", mqttClientFactory(), mqttProp.getSubscriptionTopic());
        adapter.setCompletionTimeout(mqttProp.getConnectionTimeout());
//        设置转换器
        adapter.setConverter(new DefaultPahoMessageConverter());
//        adapter.setQos(1);
//        设置通道
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * 接受的逻辑，用于监听到了消息后的自定义处理逻辑*/
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInputHandler() {
        return new ReceiveMessageHandler();
    }



//    多个Message 处理逻辑的情况

    // 1. 单个入口通道，所有消息都先到这里
//    @Bean
//    public MessageChannel mqttInputChannel() {
//        return new DirectChannel();
//    }

    // 2. 单个消息生产者，订阅所有相关主题
//    @Bean
//    public MessageProducer messageProducer() {
//        // 订阅多个主题，或使用通配符
//        String[] allTopics = {"topic/device/temp", "topic/device/alert"};
//
//        MqttPahoMessageDrivenChannelAdapter adapter =
//                new MqttPahoMessageDrivenChannelAdapter(mqttProp.getClientId() + "_input",
//                        mqttClientFactory(),
//                        allTopics);
//        adapter.setConverter(new DefaultPahoMessageConverter());
//        adapter.setOutputChannel(mqttInputChannel()); // 发送到唯一的入口通道
//        return adapter;
//    }

    // 3. 【核心】消息路由器
//    @Bean
//    @ServiceActivator(inputChannel = "mqttInputChannel")
//    public MessageRouter topicRouter() {
//        // 使用 HeaderValueRouter，根据消息头的值进行路由
//        HeaderValueRouter router = new HeaderValueRouter("mqtt_receivedTopic");
//
//        // 定义路由规则：主题 -> 通道
//        router.setChannelMapping("topic/device/temp", "tempChannel");
//        router.setChannelMapping("topic/device/alert", "alertChannel");
//
//        // 设置默认通道，如果没有匹配的规则，消息会被发送到这里
//        router.setDefaultOutputChannelName("defaultChannel");
//
//        return router;
//    }
//
//    // 4. 定义各个目标通道
//    @Bean public MessageChannel tempChannel() { return new DirectChannel(); }
//    @Bean public MessageChannel alertChannel() { return new DirectChannel(); }
//    @Bean public MessageChannel defaultChannel() { return new DirectChannel(); }
//
//    // 5. 为每个目标通道配置处理器
//    @Bean
//    @ServiceActivator(inputChannel = "tempChannel")
//    public MessageHandler tempHandler() {
//        return new TemperatureMessageHandler();
//    }
//
//    @Bean
//    @ServiceActivator(inputChannel = "alertChannel")
//    public MessageHandler alertHandler() {
//        return new AlertMessageHandler();
//    }
//
//    @Bean
//    @ServiceActivator(inputChannel = "defaultChannel")
//    public MessageHandler defaultHandler() {
//        return message -> {
//            System.out.println("[默认处理器] 收到未匹配的消息: " + message);
//        };
//    }
//}
}


//