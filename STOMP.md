# STOMP介绍 
## 一、STOMP协议介绍
STOMP即Simple (or Streaming) Text Orientated Messaging Protocol，简单(流)文本定向消息协议，它提供了一个可互操作的连接格式，允许STOMP客户端与任意STOMP消息代理（Broker）进行交互。STOMP协议由于设计简单，易于开发客户端，因此在多种语言和多种平台上得到广泛地应用。

STOMP协议的前身是TTMP协议（一个简单的基于文本的协议），专为消息中间件设计。

STOMP是一个非常简单和容易实现的协议，其设计灵感源自于HTTP的简单性。尽管STOMP协议在服务器端的实现可能有一定的难度，但客户端的实现却很容易。例如，可以使用Telnet登录到任何的STOMP代理，并与STOMP代理进行交互。

STOMP协议与2012年10月22日发布了最新的STOMP 1.2规范。
要查看STOMP 1.2规范，见： https://stomp.github.io/stomp-specification-1.2.html

## 二、STOMP的实现
业界已经有很多优秀的STOMP的服务器/客户端的开源实现，下面就介绍一下这方面的情况。

### 1、STOMP服务器（里只列了部分）
![](https://github.com/lk6678979/image/blob/master/STOMP1.jpg)
### 2、STOMP客户端库（只列了部分）
![](https://github.com/lk6678979/image/blob/master/STOMP2.jpg)


## 三、STOMP协议分析
STOMP协议与HTTP协议很相似，它基于TCP协议，使用了以下命令：

CONNECT
SEND
SUBSCRIBE
UNSUBSCRIBE
BEGIN
COMMIT
ABORT
ACK
NACK
DISCONNECT

STOMP的客户端和服务器之间的通信是通过“帧”（Frame）实现的，每个帧由多“行”（Line）组成。
第一行包含了命令，然后紧跟键值对形式的Header内容。
第二行必须是空行。
第三行开始就是Body内容，末尾都以空字符结尾。
STOMP的客户端和服务器之间的通信是通过MESSAGE帧、RECEIPT帧或ERROR帧实现的，它们的格式相似。

# 四、STOMP服务端
STOMP服务端被设计为客户端可以向其发送消息的一组目标地址。STOMP协议并没有规定目标地址的格式，它由使用协议的应用自己来定义。

# 五、STOMP客户端
对于STOMP协议来说, 客户端会扮演下列两种角色的任意一种：

* 作为生产者，通过SEND帧发送消息到指定的地址
* 作为消费者，通过发送SUBSCRIBE帧到已知地址来进行消息订阅，而当生产者发送消息到这个订阅地址后，订阅该地址的其他消费者会受到通过MESSAGE帧收到该消息
实际上，WebSocket结合STOMP相当于构建了一个消息分发队列，客户端可以在上述两个角色间转换，订阅机制保证了一个客户端消息可以通过服务器广播到多个其他客户端，作为生产者，又可以通过服务器来发送点对点消息。

# 六、STOMP帧结构
```
COMMAND
header1:value1
header2:value2

Body^@
```
^@表示行结束符

一个STOMP帧由三部分组成:命令，Header(头信息)，Body（消息体）  
* 命令使用UTF-8编码格式，命令有SEND、SUBSCRIBE、MESSAGE、CONNECT、CONNECTED等。  
* Header也使用UTF-8编码格式，它类似HTTP的Header，有content-length,content-type等。  
* Body可以是二进制也可以是文本。注意Body与Header间通过一个空行（EOL）来分隔。  
来看一个实际的帧例子
```
SEND
destination:/broker/roomId/1
content-length:57

{“type":"ENTER","content":"o7jD64gNifq-wq-C13Q5CRisJx5E"}
```
* 第1行：表明此帧为SEND帧，是COMMAND字段。
* 第2行：Header字段，消息要发送的目的地址，是相对地址。
* 第3行：Header字段，消息体字符长度。
* 第4行：空行，间隔Header与Body。
* 第5行：消息体，为自定义的JSON结构。
