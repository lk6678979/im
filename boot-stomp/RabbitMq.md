# SpringBoot+STOMP+RabbitMq实现实时通讯
## 一、SpringBoot+STOMP内存版本
https://github.com/lk6678979/im/blob/master/boot-stomp/README.md
## 二、RabbitMq插件
#### 启动RabbiutMq的Stomp插件
* 在RabbitMq的服务器上执行：
```shell
sudo rabbitmq-plugins enable rabbitmq_web_stomp
```
* 在RabbitMq的sbin目录下执行
```shell
./rabbitmq-plugins enable rabbitmq_web_stomp
```
#### 执行完后，在RabbitMq的WEB端可以看到Stomp协议端口
![](https://github.com/lk6678979/image/blob/master/STOMP6.png)
## 三、服务端实现
### 1、添加Pom依赖
```xml
<dependencies>
        <!--websocket依赖-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
               <!-- rabbitmq -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>io.projectreactor.netty</groupId>
            <artifactId>reactor-netty</artifactId>
            <version>0.8.3.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>io.projectreactor.ipc</groupId>
            <artifactId>reactor-netty</artifactId>
            <version>0.7.3.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
            <version>3.2.12.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-net</artifactId>
            <version>2.0.8.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.6.Final</version>
        </dependency>
    </dependencies>
```
### 1、启用STOMP功能（在内存队列半的基础上修改）
重写registerStompEndpoints和configureMessageBroker方法，替换内存模式的配置为rabbitmq配置
```java
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
```
### 2、 目的地路径说明
WebSocketRabbitMQMessageBrokerConfigurer中我们需要配置消息代理的前缀。在RabbitMQ中合法的目的前缀：
* /temp-queue
* /exchange
* /topic
* /queue
* /amq/queue
* /reply-queue/. 
* 注意：虚拟机需要先在MQ中提前创建好
#### 2.1  /exchange/exchangename/[routing_key]
通过交换机订阅/发布消息，交换机需要手动创建，参数说明
* /exchange：固定值(标志位，标识后面是交换机名称)
* exchangename：交换机名称
* [routing_key]：路由键，可选
```java
    /**
     * 使用@SendTo方法指定消息的目的地
     * 如果不指定@SendTo，数据所发往的目的地会与触发处理器方法的目的地相同，只不过会添加上“/topic”前缀，这个例子中就是/topic/demo2
     */
    @MessageMapping("/rdemo")
    @SendTo("/exchange/stomp-rabbitmq/demo")
    public ResponseMessage stompHandle2(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
```
在这个过程中，系统做了哪些事情呢？
* 对于接收者端，该 destination 会创建一个唯一的、自动删除的随机queue， 并根据 routing_key将该 queue 绑定到所给的 exchangename，实现对该队列的消息订阅。
* 对于发送者端，消息就会被发送到定义的 exchangename中，并且指定了 routing_key。
* mq截图：
![](https://github.com/lk6678979/image/blob/master/STOMP7.jpg)
![](https://github.com/lk6678979/image/blob/master/STOMP8.jpg)
![](https://github.com/lk6678979/image/blob/master/STOMP9.jpg)

#### 2.2  /queue/queuename
使用默认交换机订阅/发布消息，默认由stomp自动创建一个持久化队列，参数说明
* /queue：固定值
* queuename：自动创建一个持久化队列
```java
    @MessageMapping("/rdemo3")
    @SendTo("/queue/queuedemo")
    public ResponseMessage stompHandle3(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
 ```
在这个过程中，系统做了哪些事情呢？
* 对于接收者端，订阅队列queuename的消息
* 对于接收者端，向queuename发送消息
* destination 只会在第一次发送消息的时候会自动创建一个持久化队列,队列名称queuename，绑定默认的交换机
![](https://github.com/lk6678979/image/blob/master/STOMP10.jpg)
![](https://github.com/lk6678979/image/blob/master/STOMP11.jpg)

#### 2.3  /amq/queue/queuedemo
和上文的”/queue/queuename”相似，两者的区别是
* 与/queue/queuename的区别在于队列不由stomp自动进行创建，队列不存在失败
```java
    @MessageMapping("/rdemo4")
    @SendTo("/amq/queue/queuedemo")
    public ResponseMessage stompHandle4(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
 ```
* destination 会实现对队列的消息订阅。 对于 SEND frame，消息会通过默认的 exhcange 直接被发送到队列中
* 这种情况下无论是发送者还是接收者都不会产生队列。 但如果该队列不存在，接收者会报错。
* 注意：如果代码中配置的/amq的队列在mq中没有创建，整个snomp都无法正常使用，不止是这个队列无法使用
```java
2019-11-26 14:20:59.571 ERROR 227300 --- [ent-scheduler-6] o.s.m.s.s.StompBrokerRelayMessageHandler : Received ERROR {message=[not_found], content-type=[text/plain], version=[1.0,1.1,1.2], content-length=[53]} session=4a2xepfz, user=21 text/plain payload=NOT_FOUND - no queue 'queuedemo' in vhost '/test-im'
```
#### 2.4  /topic/routing_key
通过amq.topic交换机订阅/发布消息，订阅时默认创建一个临时队列，通过routing_key与topic进行绑定
* /topic：固定前缀
* routing_key：路由键
```java
    @MessageMapping("/rdemo5")
    @SendTo("/topic/routing_demo")
    public ResponseMessage stompHandle5(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
 ```
* 对于发送者端，会创建出自动删除的、非持久的队列并根据 routing_key路由键绑定到 amq.topic 交换机 上，同时实现对该队列的订阅。
* 对于发送者端，消息会被发送到 amq.topic 交换机中。
![](https://github.com/lk6678979/image/blob/master/STOMP12.jpg)
![](https://github.com/lk6678979/image/blob/master/STOMP13.jpg)
![](https://github.com/lk6678979/image/blob/master/STOMP14.jpg)
### 3. JS客户端实现
https://github.com/lk6678979/im/blob/master/StompJS%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3%E6%80%BB%E7%BB%93.md
### 4. 前端html页面（改html中的访问地址即可）
https://github.com/lk6678979/im/blob/master/boot-stomp/src/main/resources/static/stomp.html

### 5. 补充
后端没有配置往前端发送心跳，配置方式
```
@Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //设置简单的消息代理器，它使用Memory（内存）作为消息代理器，
        //其中/user和/topic都是我们发送到前台的数据前缀。前端必须订阅以/user开始的消息（.subscribe()进行监听）。
        //setHeartbeatValue设置后台向前台发送的心跳，
        //注意：setHeartbeatValue这个不能单独设置，不然不起作用，要配合后面setTaskScheduler才可以生效。
        //对应的解决方法的网址：https://stackoverflow.com/questions/39220647/spring-stomp-over-websockets-not-scheduling-heartbeats
        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1);
        te.setThreadNamePrefix("wss-heartbeat-thread-");
        te.initialize();
        registry.enableSimpleBroker("/user","/topic").setHeartbeatValue(new long[]{HEART_BEAT,HEART_BEAT}).setTaskScheduler(te);;
        //设置我们前端发送：websocket请求的前缀地址。即client.send("/ws-send")作为前缀，然后再加上对应的@MessageMapping后面的地址
        registry.setApplicationDestinationPrefixes("/ws-send");
    }
}
```
我们只要配置setHeartbeatValue(new long[]{HEART_BEAT,HEART_BEAT}).setTaskScheduler(te);这句话就可以了。前一个是配置心跳，后一个使用一个线程发送心跳。
