package com.owp.boot.stomp;

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
