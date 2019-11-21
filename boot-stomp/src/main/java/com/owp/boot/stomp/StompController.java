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
public class StompController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 不实用@SendTo，使用SimpMessagingTemplate发送消息
     */
    @MessageMapping("/demo")
    public void stompHandle(Principal principal, RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        String sender = principal.getName();
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(sender);
        //目的地要写全路径，不能省略前缀
        simpMessagingTemplate.convertAndSend("/topic/demo", responseMessage);
    }

    /**
     * 使用@SendTo方法指定消息的目的地
     * 如果不指定@SendTo，数据所发往的目的地会与触发处理器方法的目的地相同，只不过会添加上“/topic”前缀，这个例子中就是/topic/demo2
     */
    @MessageMapping("/demo2")
    @SendTo("/topic/demo")
    public ResponseMessage stompHandle2(RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    /***
     * 直接返回数据给客户端
     */
    @SubscribeMapping("/demo3")
    public ResponseMessage stompHandle3(RequestMessage requestMessage) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    /***
     * 使用@SendTo方法指定消息的目的地，不直接发送给客户端
     */
    @SubscribeMapping("/demo4")
    @SendTo("/topic/demo")
    public ResponseMessage stompHandle4(RequestMessage requestMessage) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    @MessageMapping("/spittle")
    @SendToUser("/queue/notifications")
    public ResponseMessage handleSpittle(Principal principal, RequestMessage requestMessage) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        return responseMessage;
    }

    @MessageMapping("/singleShout")
    public void singleUser(RequestMessage requestMessage, StompHeaderAccessor stompHeaderAccessor) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(requestMessage.getSender());
        Principal user = stompHeaderAccessor.getUser();
        //目的地不要加registry.setUserDestinationPrefix设置的前缀
        simpMessagingTemplate.convertAndSendToUser(user.getName(), "/queue/notifications", responseMessage);
    }

    @MessageMapping("/demoPlus")
    public void stompHandlePlus(Principal principal, RequestMessage requestMessage) throws MessagingException, UnsupportedEncodingException {
        String sender = principal.getName();
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setContent(requestMessage.getContent());
        responseMessage.setSender(sender);
        if ("1".equals(requestMessage.getType())) {//点对点
            //目的地不要加registry.setUserDestinationPrefix设置的前缀
            simpMessagingTemplate.convertAndSendToUser(requestMessage.getReceiver(), requestMessage.getTopic(), responseMessage);
        } else {
            //目的地要写全路径，不能省略前缀
            simpMessagingTemplate.convertAndSend("/topic/" + requestMessage.getTopic(), responseMessage);
        }
    }
}
