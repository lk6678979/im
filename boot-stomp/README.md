#
## 一、STOMP 简介
https://github.com/lk6678979/im/blob/master/STOMP.md
## 二、服务端实现
### 1、启用STOMP功能
STOMP 的消息根据前缀的不同分为三种。如下，
* 使用setApplicationDestinationPrefixes方法申明的前缀url(可以设置多个)，该前缀的客户端请求都会被路由到带有@MessageMapping 或 @SubscribeMapping 注解的方法中，可以理解为服务端处理客户端请求的目的地方法的前缀；
* 使用setApplicationDestinationPrefixes方法的例子：设置前缀为/app，当客户发送/app/demo的请求时，消息会被路由到带有@MessageMapping("/demo") 或 @SubscribeMapping("/demo")注解的方法中
* 使用enableSimpleBroker方法申明的前缀url，定义客户端可订阅的STOMP代理的消息目的地前缀，发送到STOMP代理消息目的地中的消息会推送给订阅的客户端，根据你所选择的STOMP代理不同，目的地的可选前缀也会有所限制；
* 使用setUserDestinationPrefix方法申明的前缀url，会将消息重路由到某个用户独有的目的地上。
![](https://github.com/lk6678979/image/blob/master/STOMP4.jpg)
```java
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

    /**
     * 将 "/stomp" 注册为一个 STOMP 端点。这个路径与之前发送和接收消息的目的地路径有所
     * 不同。这是一个端点，客户端在订阅或发布消息到目的地路径前，要连接到该端点。
     */
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
         * 客户端发送消息的请求的前缀，客户端使用这个前端的url往服务器发送消息
         * 当客户发送   前缀/url   的请求时，消息会被路由到带有@MessageMapping("/url") 或 @SubscribeMapping("/url")注解的方法中
         * 使用 @MessageMapping 或者 @SubscribeMapping 注解可以处理客户端发送过来的消息，并选择方法是否有返回值。
         * 如果 @MessageMapping 注解的控制器方法有返回值的话，返回值会被发送到消息代理，只不过会添加上"/topic"前缀。可以使用@SendTo 重写消息目的地；
         * 如果 @SubscribeMapping 注解的控制器方法有返回值的话，返回值会直接发送到客户端，不经过代理。如果加上@SendTo 注解的话，则要经过消息代理。
         * 例如：客户端发送请求https://host:ip/app/demo,那么用来路由的url就是 /demo
         */
        registry.setApplicationDestinationPrefixes("/app","foo");//这里翻译过来的意思是APP发送请求到服务端的目的地的前缀，也就是需要服务端处理的请求的前缀
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
```
### 2、编写写应用接收消息和发送消息实体类
#### 2.1 客户端发送消息的请求类
```java
public class RequestMessage {

    private String sender;//消息发送者
    private String room;//房间号
    private String type;//消息类型
    private String content;//消息内容

    public RequestMessage() {
    }

    public RequestMessage(String sender, String room, String type, String content) {
        this.sender = sender;
        this.room = room;
        this.type = type;
        this.content = content;
    }


    public String getSender() {
        return sender;
    }

    public String getRoom() {
        return room;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }


    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String room) {
        this.room = room;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
```
#### 2.2 服务端发送给客户端消息的响应类
```java
public class ResponseMessage {

    private String sender;//发送者
    private String type;//类型
    private String content;//内容

    public ResponseMessage() {
    }

    public ResponseMessage(String sender, String type, String content) {
        this.sender = sender;
        this.type = type;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }


    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
```
### 3、 处理来自客户端的STOMP消息
服务端处理客户端发来的STOMP消息，主要使用 @MessageMapping 或者 @SubscribeMapping
#### 3.1 使用@MessageMapping
```java
    /**
     * 使用@SendTo方法指定消息的目的地
     * 如果不指定@SendTo，数据所发往的目的地会与触发处理器方法的目的地相同，只不过会添加上“/topic”前缀，这个例子中就是/topic/demo
     */
    @MessageMapping("/demo")
    @SendTo("/topic/demo")
    public ResponseMessage stompHandle2(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
```
* @MessageMapping 指定的客户端请求目的地是“/app/demo”（“/app”前缀是隐含的，因为我们将其配置为应用的目的地前缀），会将方法的出参写到@SendTo指定的目的地中，订阅这个目的地的客户端能接收到消息
* 方法接收一个RequestMessage参数，因为Spring的某一个消息转换器会将STOMP消息的负载转换为RequestMessage对象。Spring 4.0提供了几个消息转换器，作为其消息API的一部分：
![](https://github.com/lk6678979/image/blob/master/STOMP5.jpg)
* 尤其注意，这个处理器方法有一个返回值，这个返回值并不是返回给客户端的，而是转发给消息代理的，如果客户端想要这个返回值的话，只能从消息代理订阅。@SendTo 注解重写了消息代理的目的地，如果不指定@SendTo，数据所发往的目的地会与触发处理器方法的目的地相同，只不过会添加上“/topic”前缀，这个例子中就是/topic/demo。
#### 3.2 使用@SubscribeMapping
```
    /***
     * 直接返回数据给客户端
     */
    @SubscribeMapping("/demo")
    public ResponseMessage stompHandle3(RequestMessage requestMessage) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
```
* @SubscribeMapping的主要应用场景是实现请求-回应模式。在请求-回应模式中，客户端订阅某一个目的地，然后预期在这个目的地上获得一个一次性的响应。
* 使用了@SubscribeMapping注解，用这个方法来处理对“/app/demo”目的地的订阅（与@MessageMapping类似，“/app”是隐含的）。当处理这个订阅时，方法会产生一个输出的ResponseMessage对象并将其返回。然后，ResponseMessage对象会转换成一条消息，并且会按照客户端订阅时相同的目的地发送回客户端。
* 如果你觉得这种请求-回应模式与HTTP GET的请求-响应模式里的关键区别在于HTTPGET请求是同步的，而订阅的请求-回应模式则是异步的，这样客户端能够在回应可用时再去处理，而不必等待。
### 4、 发送消息到客户端
#### 4.1 在处理消息之后发送消息
* 正如前面看到的那样，使用 @MessageMapping 或者 @SubscribeMapping 注解可以处理客户端发送过来的消息，并选择方法是否有返回值。
* 如果 @MessageMapping 注解的控制器方法有返回值的话，返回值会被发送到消息代理，只不过会添加上"/topic"前缀。可以使用@SendTo 重写消息目的地；
* 如果 @SubscribeMapping 注解的控制器方法有返回值的话，返回值会直接发送到客户端，不经过代理。如果加上@SendTo 注解的话，则要经过消息代理。
#### 4.2 在应用的任意地方发送消息
spring-websocket 定义了一个 SimpMessageSendingOperations 接口（或者使用SimpMessagingTemplate ），可以实现自由的向任意目的地发送消息，并且订阅此目的地的所有用户都能收到消息。
```java
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 不实用@SendTo，使用SimpMessagingTemplate发送消息
     */
    @MessageMapping("/demo")
    public void stompHandle(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        simpMessagingTemplate.convertAndSend("/topic/demo", responseMessage);
    }
```
#### 4.3 为指定用户发送消息
如果你知道用户是谁的话，那么就能处理与某个用户相关的消息，而不仅仅是与所有客户端相关联。我们可以使用Spring Security来认证用户，并为目标用户处理消息。在使用Spring和STOMP消息功能的时候，我们有三种方式利用认证用户：
* @MessageMapping和@SubscribeMapping标注的方法能够使用Principal来获取认证用户(当前发送请求的用户）；
* @MessageMapping、@SubscribeMapping和@MessageException方法返回的值能够以消息的形式发送给入参Principal对象的认证用户（也就是当前请求的用户）；
* SimpMessagingTemplate能够发送消息给特定用户。
##### 4.3.1 如何为每个客户端绑定Principal?

##### 4.3.2 在控制器中处理用户的消息
在控制器的@MessageMapping或@SubscribeMapping方法中，处理消息时有两种方式了解用户信息。在处理器方法中，通过简单地添加一个Principal参数，这个方法就能知道用户是谁并利用该信息关注此用户相关的数据。除此之外，处理器方法还可以使用@SendToUser注解或者使用SimpMessageSendingOperations 接口的convertAndSendToUser方法，表明它的返回值要以消息的形式发送给某个认证用户的客户端（只发送给该客户端）。
###### 4.3.2.1 基于@SendToUser注解和Principal参数
```java
  @MessageMapping("/spittle")
  @SendToUser("/queue/notifications")
  public Notification handleSpittle(Principal principal, SpittleForm form) {
      Spittle spittle = new Spittle(principal.getName(), form.getText(), new Date());
      spittleRepo.save(spittle);
      feedService.broadcastSpittle(spittle);
      return new Notification("Saved Spittle for user: " + principal.getName());
  }
```
JavaScript客户端订阅目的地的代码：
```js
stomp.subscribe("/user/queue/notifications", handleNotification);
```
* 在内部，以“/user”作为前缀的目的地将会以特殊的方式进行处理。这种消息不会通过AnnotationMethodMessageHandler（像应用消息那样）来处理，也不会通过SimpleBrokerMessageHandler或StompBrokerRelayMessageHandler（像代理消息那样）来处理，以“/user”为前缀的消息将会通过UserDestinationMessageHandler进行处理
* @SendToUser 表示要将消息发送给指定的用户，会自动在消息目的地前补上"/user"前缀。例如这里的消息目的地是/user/queue/notifications
###### 4.3.2.2 为指定用户发送消息convertAndSendToUser
除了convertAndSend()以外，SimpMessageSendingOperations 还提供了convertAndSendToUser()方法。按照名字就可以判断出来，convertAndSendToUser()方法能够让我们给特定用户发送消息。
```java
@MessageMapping("/singleShout")
    public void singleUser(RequestMessage requestMessage, StompHeaderAccessor stompHeaderAccessor) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        Principal user = stompHeaderAccessor.getUser();
        simpMessageSendingOperations.convertAndSendToUser(user.getName(), "/queue/notifications", responseMessage);
    }
 ```
* 如上，这里虽然我还是用了认证的信息得到用户名。但是，其实大可不必这样，因为 convertAndSendToUser 方法可以指定要发送给哪个用户。也就是说，完全可以把用户名的当作一个参数传递给控制器方法，从而绕过身份认证！convertAndSendToUser 方法最终会把消息发送到 /user/username/queue/notifications 目的地上。也就是前端订阅了/user/queue/notifications的客户端能消费到
