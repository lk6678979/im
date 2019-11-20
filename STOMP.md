# STOMP介绍 
作者：chszs，转载需注明。博客主页：http://blog.csdn.net/chszs
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


#@三、STOMP协议分析
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
