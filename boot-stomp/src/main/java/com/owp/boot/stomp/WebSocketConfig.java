package com.owp.boot.stomp;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * 通过EnableWebSocketMessageBroker 开启使用STOMP协议来传输基于代理(message broker)的消息,
 * 此时使用@MessageMapping 就像支持@RequestMapping一样。
 * 写在前面：客户端如何注册？
 * 1.客户端API会先登录
 * 2.然后客户端使用subscribe方法来订阅目的地，这个过程后台不用管，框架自己实现
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        //将客户端标识封装为Principal对象，从而让服务端能通过getName()方法找到指定客户端
                        Object o = attributes.get("name");
                        return new FastPrincipal(o.toString());
                    }
                })
                //添加socket拦截器，用于从请求中获取客户端标识参数
                .addInterceptors(new HandleShakeInterceptors()).withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /**
         * 客户端发送消息的请求的前缀，客户端使用这个前端的url往服务器发送消息（主要用来发送消息和订阅目的地）
         * 当客户发送   前缀/url   的请求时，消息会被路由到带有@MessageMapping("/url") 或 @SubscribeMapping("/url")注解的方法中
         * 使用 @MessageMapping 或者 @SubscribeMapping 注解可以处理客户端发送过来的消息，并选择方法是否有返回值。
         * 如果 @MessageMapping 注解的控制器方法有返回值的话，返回值会被发送到消息代理，只不过会添加上"/topic"前缀。可以使用@SendTo 重写消息目的地；
         * 如果 @SubscribeMapping 注解的控制器方法有返回值的话，返回值会直接发送到客户端，不经过代理。如果加上@SendTo 注解的话，则要经过消息代理。
         * 例如：客户端发送请求https://host:ip/app/demo,那么用来路由的url就是 /demo
         */
        registry.setApplicationDestinationPrefixes("/app");//这里翻译过来的意思是APP发送请求到服务端的目的地的前缀，也就是需要服务端处理的请求的前缀
        //客户端订阅消息的请求前缀，topic一般用于广播推送，queue用于点对点推送
        /**
         * 定义了一个客户端订阅地址的前缀信息（告诉服务器，我要订阅哪个目的地的url前缀，当该目的地有消息时，会主动推送给客户端）
         * topic一般用于广播推送，queue用于点对点推送（你也可以不用这2个，随意指定）
         * 例如客户端发送请求https://host:ip/topic/order，那么客户端实际订阅的目的地是/topic/order
         * 客户端使用subscribe方法来订阅目的地，这个过程后台不用管，框架自己实现
         */
        registry.enableSimpleBroker("/topic", "/queue");
        //点对点使用的订阅前缀（客户端订阅路径上会体现出来），不设置的话，默认也是/user/
        registry.setUserDestinationPrefix("/user");
        /*  如果是用自己的消息中间件，则按照下面的去配置，删除上面的配置
         *   registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("rabbit.someotherserver")
            .setRelayPort(62623)
            .setClientLogin("marcopolo")
            .setClientPasscode("letmein01");
            registry.setApplicationDestinationPrefixes("/app", "/foo");
         * */


    }

    //定义一个自己的权限验证类
    class FastPrincipal implements Principal {

        private final String name;

        public FastPrincipal(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
