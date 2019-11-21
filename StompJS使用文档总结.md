转自 https://www.cnblogs.com/goloving/p/10746378.html

## 一、创建STOMP客户端

### 1、在web浏览器中使用普通的Web Socket
STOMP javascript 客户端会使用ws://的URL与STOMP 服务端进行交互。  
为了创建一个STOMP客户端js对象，你需要使用Stomp.client(url)，而这个URL连接着服务端的WebSocket的代理
```js
var url = "ws://localhost:61614/stomp";
var client = Stomp.client(url);
Stomp.client(url, protocols)
```
也可以用来覆盖默认的subprotocols。第二个参数可以是一个字符串或一个字符串数组去指定多个subprotocols。
### 2、在web浏览器中使用定制的WebSocket
* 浏览器提供了不同的WebSocket的协议，一些老的浏览器不支持WebSocket的脚本或者使用别的名字。默认下，stomp.js会使用浏览器原生的WebSocket class去创建WebSocket。
* 但是利用Stomp.over(ws)这个方法可以使用其他类型的WebSockets。这个方法得到一个满足WebSocket定义的对象。例如，可以使用由SockJS实现的Websocket。
* 如果使用原生的Websockets就使用Stomp.client(url)，如果需要使用其他类型的Websocket（例如由SockJS包装的Websocket）就使用Stomp.over(ws)。除了初始化有差别，Stomp API在这两种方式下是相同的。
### 3、在node.js程序中

* 通过stompjs npm package同样也可以在node.js程序中使用这个库。
```js
npm install stompjs
```
* 在node.js app中，require这个模块：

```js
var Stomp = require('stompjs');
```

* 为了与建立在TCP socket的STOMP-broker连接，使用Stomp.overTCP(host, port)方法。

```js
var client = Stomp.overTCP('localhost', 61613);
```　　

* 为了与建立在Web Socket的STOMP broker连接，使用Stomp.overWS(url)方法。

```js
var client = Stomp.overWS('ws://localhost:61614/stomp');
```

* 除了初始化不同，无论是浏览器还是node.js环境下，Stomp API都是相同的。

## 二、链接服务端
   一旦Stomp 客户端建立了，必须调用它的connect()方法去连接Stomp服务端进行验证。这个方法需要两个参数，用户的登录和密码凭证。这种情况下，客户端会使用Websocket打开连接，并发送一个CONNECT frame。  
   这个连接是异步进行的：你不能保证当这个方法返回时是有效连接的。为了知道连接的结果，你需要一个回调函数。
```js
var connect_callback = function() {
    // called back after the client is connected and authenticated to the STOMP server
};
```
* 但是如果连接失败会发生什么呢？
connect()方法接受一个可选的参数(error_callback)，当客户端不能连接上服务端时，这个回调函数error_callback会被调用，该函数的参数为对应的错误对象。  
```js
var error_callback = function(error) {
    // display the error's message header:
    alert(error.headers.message);
};
```
* 在大多数情况下，connect()方法可接受不同数量的参数来提供简单的API：
```js
client.connect(login, passcode, connectCallback);
client.connect(login, passcode, connectCallback, errorCallback);
client.connect(login, passcode, connectCallback, errorCallback, host);
login和passcode是strings，connectCallback和errorCallback则是functions。（有些brokers（代理）还需要传递一个host（String类型）参数。）
```
* 如果你需要附加一个headers头部，connect方法还接受其他两种形式的参数：
```js
client.connect(headers, connectCallback);
client.connect(headers, connectCallback, errorCallback);
```
* header是map形式，connectCallback和errorCallback为functions。
* 需要注意：如果你使用上述这种方式，你需要自行在headers添加login、passcode（甚至host）：
```js
var headers = {
    login: 'mylogin',
    passcode: 'mypasscode',
    // additional header
    'client-id': 'my-client-id'
};
client.connect(headers, connectCallback);
```
* 断开连接时，调用disconnect方法，这个方法也是异步的，当断开成功后会接收一个额外的回调函数的参数。如下所示。
```js
client.disconnect(function() {
    alert("See you next time!");
};
```
* 当客户端与服务端断开连接，就不会再发送或接收消息了。

## 三、Heart-beating

* 如果STOMP broker(代理)接收STOMP 1.1版本的帧，heart-beating是默认启用的。
* heart-beating也就是频率，incoming是接收频率，outgoing是发送频率。
* 通过改变incoming和outgoing可以更改客户端的heart-beating(默认为10000ms)：
```js
client.heartbeat.outgoing = 20000; 
// client will send heartbeats every 20000ms
client.heartbeat.incoming = 0;
// client does not want to receive heartbeats
// from the server
　　heart-beating是利用window.setInterval()去规律地发送heart-beats或者检查服务端的heart-beats。
```
## 四、发送消息
　　当客户端与服务端连接成功后，可以调用send()来发送STOMP消息。这个方法必须有一个参数，用来描述对应的STOMP的目的地。另外可以有两个可选的参数：headers，object类型包含额外的信息头部；body，一个String类型的参数。
```js
client.send("/queue/test", {priority: 9}, "Hello, STOMP");
// client会发送一个STOMP发送帧给/queue/test，这个帧包含一个设置了priority为9的header和内容为“Hello, STOMP”的body。
　　client.send(destination, {}, body);
```
* 如果你想发送一个有body的信息，也必须传递headers参数。如果没有headers需要传递，那么就传{}即可。

## 五、订阅（Subscribe）和接收（receive）消息
  为了在浏览器中接收消息，STOMP客户端必须先订阅一个目的地destination。  

  你可以使用subscribe()去订阅。这个方法有2个必需的参数：目的地(destination)，回调函数(callback)；还有一个可选的参数headers。其中destination是String类型，对应目的地，回调函数是伴随着一个参数的function类型。  
```js
var subscription = client.subscribe("/queue/test", callback);
```
* subscribe()方法返回一个object，这个object包含一个id属性，对应这个这个客户端的订阅ID。
* 而unsubscribe()可以用来取消客户端对这个目的地destination的订阅。
* 默认情况下，如果没有在headers额外添加，这个库会默认构建一个独一无二的ID。在传递headers这个参数时，可以使用你自己的ID。
```js
var mysubid = '...';
var subscription = client.subscribe(destination, callback, { id: mysubid });
```
　　这个客户端会向服务端发送一个STOMP订阅帧（SUBSCRIBE frame）并注册回调事件。每次服务端向客户端发送消息时，客户端都会轮流调用回调函数，参数为对应消息的STOMP帧对象（Frame object）。  
subscribe()方法，接受一个可选的headers参数用来标识附加的头部。
```js
var headers = {ack: 'client', 'selector': "location = 'Europe'"};
client.subscribe("/queue/test", message_callback, headers);
```
* 这个客户端指定了它会确认接收的信息，只接收符合这个selector : location = 'Europe'的消息。
* 如果想让客户端订阅多个目的地，你可以在接收所有信息的时候调用相同的回调函数：
```js
onmessage = function(message) {
    // called every time the client receives a message
}
var sub1 = client.subscribe("queue/test", onmessage);
var sub2 = client.subscribe("queue/another", onmessage)
```
* 如果要中止接收消息，客户端可以在subscribe()返回的object对象调用unsubscribe()来结束接收。
```js
var subscription = client.subscribe(...);
...
subscription.unsubscribe();
```
## 六、支持JSON
 STOMP消息的body必须为字符串。如果你需要发送/接收JSON对象，你可以使用JSON.stringify()和JSON.parse()去转换JSON对象。
## 七、Acknowledgment(确认)
默认情况，在消息发送给客户端之前，服务端会自动确认（acknowledged）。  
客户端可以选择通过订阅一个目的地时设置一个ack header为client或client-individual来处理消息确认。  
在下面这个例子，客户端必须调用message.ack()来通知服务端它已经接收了消息。  
```js
var subscription = client.subscribe("/queue/test",
    function(message) {
        // do something with the message
        ...
        // and acknowledge it
        message.ack();
    },
    {ack: 'client'}
);
```
* ack()接受headers参数用来附加确认消息。例如，将消息作为事务(transaction)的一部分，当要求接收消息时其实代理（broker）已经将ACK STOMP frame处理了。
```js
var tx = client.begin();
message.ack({ transaction: tx.id, receipt: 'my-receipt' });
tx.commit();
```
* nack()也可以用来通知STOMP 1.1.brokers（代理）：客户端不能消费这个消息。与ack()方法的参数相同。

## 八、事务(Transactions)
可以在将消息的发送和确认接收放在一个事务中。
  客户端调用自身的begin()方法就可以开始启动事务了，begin()有一个可选的参数transaction，一个唯一的可标识事务的字符串。如果没有传递这个参数，那么库会自动构建一个。这个方法会返回一个object。这个对象有一个id属性对应这个事务的ID，还有两个方法：  
* commit()提交事务
* abort()中止事务
在一个事务中，客户端可以在发送/接受消息时指定transaction id来设置transaction。
```js
// start the transaction
var tx = client.begin();
// send the message in a transaction
client.send("/queue/test", {transaction: tx.id}, "message in a transaction");
// commit the transaction to effectively send the message
tx.commit();
```
* 如果你在调用send()方法发送消息的时候忘记添加transction header，那么这不会称为事务的一部分，这个消息会直接发送，不会等到事务完成后才发送。
```js
var txid = "unique_transaction_identifier";
// start the transaction
var tx = client.begin();
// oops! send the message outside the transaction
client.send("/queue/test", {}, "I thought I was in a transaction!");
tx.abort(); // Too late! the message has been sent
```
## 九、调试
有一些测试代码能有助于你知道库发送或接收的是什么，从而来调试程序。  
客户端可以将其debug属性设置为一个函数，传递一个字符串参数去观察库所有的debug语句。默认情况，debug消息会被记录在在浏览器的控制台。  
```js
client.debug = function(str) {
    // append the debug log to a #debug div somewhere in the page using JQuery:
    $("#debug").append(str + "\n");
};
```
## 十、使用情况
```js
* 1、var error_callback = function(error) {
　　第一次连接失败和连接后断开连接都会调用这个函数
};
```
* 2、关闭控制台调试数据：设置client.debug = null 就可以，stompjs会去检测debug是否是函数，不是函数就不会调用输出
