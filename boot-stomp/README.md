#
## 一、STOMP 简介
https://github.com/lk6678979/im/blob/master/STOMP.md
## 二、服务端实现
### 1、启用STOMP功能
STOMP 的消息根据前缀的不同分为三种。如下，
* 使用setApplicationDestinationPrefixes方法申明的前缀url，该前缀的客户端请求都会被路由到带有@MessageMapping 或 @SubscribeMapping 注解的方法中，可以理解为服务端处理客户端请求的目的地方法的前缀；当客户发送   前缀/url   的请求时，消息会被路由到带有@MessageMapping("/url") 或 @SubscribeMapping("/url")注解的方法中
* 使用enableSimpleBroker方法申明的前缀url，定义客户端可订阅的STOMP代理的消息目的地前缀，发送到STOMP代理消息目的地中的消息会推送给订阅的客户端，根据你所选择的STOMP代理不同，目的地的可选前缀也会有所限制；
* 使用setUserDestinationPrefix方法申明的前缀url，会将消息重路由到某个用户独有的目的地上。
![](https://github.com/lk6678979/image/blob/master/STOMP4.jpg)
