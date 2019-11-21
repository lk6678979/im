package com.owp.boot.stomp;


public class RequestMessage {

    private String sender;//消息发送者
    private String receiver;//接受者
    private String topic;//主题
    private String type;//消息类型，1：点对点，2：主题订阅
    private String content;//消息内容

    public RequestMessage() {
    }

    public RequestMessage(String sender, String receiver, String topic, String type, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.topic = topic;
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

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}