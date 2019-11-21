package com.owp.boot.stomp;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;

@Component
public class ConnectParamInterceptor extends ChannelInterceptorAdapter {

    /**
     * org.springframework.messaging.simp.stomp.StompCommand支持的场景都可以拦截
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        //处理客户端发起连接的场景
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            //从head中回去数据
            Object raw = message.getHeaders().get(SimpMessageHeaderAccessor.NATIVE_HEADERS);
            //这里也可以让前端传token校验
            if (raw instanceof Map) {
                Object usernameObj = ((Map) raw).get("username");
                Object passwordObj = ((Map) raw).get("password");
                if (usernameObj instanceof LinkedList && passwordObj instanceof LinkedList) {
                    String username = ((LinkedList) usernameObj).get(0).toString();
                    String password = ((LinkedList) passwordObj).get(0).toString();
                    // 设置当前访问的认证用户
                    accessor.setUser(new FastPrincipal(username));
                } else {
                    return null;//返回null，则登录不成功
                }
            }
        }
        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            System.out.println("==========登出==========");
        }
        return message;
    }
}
