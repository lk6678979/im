package com.owp.boot.stomp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

@RestController
public class RabbitmqStompController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 不实用@SendTo，使用SimpMessagingTemplate发送消息
     */
    @MessageMapping("/rdemo")
    public void stompHandle(Principal principal, RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        String sender = principal.getName();
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(sender);
        //目的地要写全路径，不能省略前缀
        simpMessagingTemplate.convertAndSend("/exchange/stomp-rabbitmq/demo", responseMessage);
    }

    /**
     * 使用@SendTo方法指定消息的目的地
     * 如果不指定@SendTo，数据所发往的目的地会与触发处理器方法的目的地相同，只不过会添加上“/topic”前缀，这个例子中就是/topic/demo2
     */
    @MessageMapping("/rdemo2")
    @SendTo("/exchange/stomp-rabbitmq/demo")
    public ResponseMessage stompHandle2(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    @MessageMapping("/rdemo3")
    @SendTo("/queue/queuedemo")
    public ResponseMessage stompHandle3(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    @MessageMapping("/rdemo4")
    @SendTo("/amq/queue/queuedemo")
    public ResponseMessage stompHandle4(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    @MessageMapping("/rdemo5")
    @SendTo("/topic/routing_demo")
    public ResponseMessage stompHandle5(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }
}
