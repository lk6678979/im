package com.owp.boot.stomp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * 通过EnableWebSocketMessageBroker 开启使用STOMP协议来传输基于代理(message broker)的消息,
 * 写在前面：客户端如何注册？
 * 1.客户端API会先登录
 * 2.然后客户端使用subscribe方法来订阅目的地，这个过程后台不用管，框架自己实现
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketRabbitMqConfig extends AbstractWebSocketMessageBrokerConfigurer {


    @Autowired
    private ConnectParamInterceptor getHeaderParamInterceptor;

    /**
     * 将 "/stomp" 注册为一个 STOMP 端点。这个路径与之前发送和接收消息的目的地路径有所
     * 不同。这是一个端点，客户端在订阅或发布消息到目的地路径前，要连接到该端点。
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp")
                //添加socket拦截器，用于握手前和握手后调用
                .addInterceptors(new HandleShakeInterceptors()).withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /**
         * 客户端发送消息的请求的前缀，客户端使用这个前缀的url往服务器发送消息
         * 当客户发送   前缀/url   的请求时，消息会被路由到带有@MessageMapping("/url") 或 @SubscribeMapping("/url")注解的方法中
         * 使用 @MessageMapping 或者 @SubscribeMapping 注解可以处理客户端发送过来的消息，并选择方法是否有返回值。
         * 如果 @MessageMapping 注解的控制器方法有返回值的话，返回值会被发送到消息代理，只不过会添加上"/topic"前缀。可以使用@SendTo 重写消息目的地；
         * 如果 @SubscribeMapping 注解的控制器方法有返回值的话，返回值会直接发送到客户端，不经过代理。如果加上@SendTo 注解的话，则要经过消息代理。
         * 例如：客户端发送请求https://host:ip/app/demo,那么用来路由的url就是 /demo
         */
        registry.setApplicationDestinationPrefixes("/app", "foo");//这里翻译过来的意思是APP发送请求到服务端的目的地的前缀，也就是需要服务端处理的请求的前缀
        /**
         * 定义了一个客户端订阅地址的前缀信息（告诉服务器，我要订阅哪个目的地的url前缀，当该目的地有消息时，会主动推送给客户端）
         * topic一般用于广播推送，queue用于点对点推送（你也可以不用这2个，随意指定）
         * 例如客户端发送请求https://host:ip/topic/order，那么客户端实际订阅的目的地是/topic/order
         * 客户端使用subscribe方法来订阅目的地，这个过程后台不用管，框架自己实现
         */
        registry.enableStompBrokerRelay("/topic", "/queue","/temp-queue","/exchange","/amq/queue","/reply-queue/.")
                .setRelayHost("kafka01")
                .setVirtualHost("/test-im")
                .setRelayPort(61613)
                .setClientLogin("sziov")
                .setClientPasscode("sziov")
                .setSystemLogin("sziov")
                .setSystemPasscode("sziov")
                .setSystemHeartbeatSendInterval(5000)
                .setSystemHeartbeatReceiveInterval(4000);
    }

    /**
     * 客户端通过client连接服务器时绑定通道的处理逻辑
     * 这里是添加了一个拦截器，拦截器中可以针对连接、取消连接、发送消息都客户端行为进行逻辑处理
     *
     * @param registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(getHeaderParamInterceptor);
    }
}
