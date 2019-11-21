package com.owp.boot.stomp;

import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

/**
 * 握手前和握手后调用
 */
public class HandleShakeInterceptors implements HandshakeInterceptor {

    /**
     * 在握手之前执行该方法, 继续握手返回true, 中断握手返回false.
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        System.out.println("==========开始握手==========");
        return true;
    }

    /**
     * 在握手之后执行该方法. 无论是否握手成功都指明了响应状态码和相应头.
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param exception
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        System.out.println("==========结束握手==========");
    }

}