package shareyourstory.websocket.service;

public class PrivateConversationDTO {
    private String userId;
    private String username;
    private String lastMessage;
    private String lastTime;

    public PrivateConversationDTO() {}

    public PrivateConversationDTO(String userId, String username, String lastMessage, String lastTime) {
        this.userId = userId;
        this.username = username;
        this.lastMessage = lastMessage;
        this.lastTime = lastTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }
}
