package shareyourstory.websocket.service;

public class CommunityMessageDTO {
    private String id;
    private String username;
    private String text;
    private String time;
    private boolean own;

    public CommunityMessageDTO() {}

    public CommunityMessageDTO(String id, String username, String text, String time, boolean own) {
        this.id = id;
        this.username = username;
        this.text = text;
        this.time = time;
        this.own = own;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isOwn() {
        return own;
    }

    public void setOwn(boolean own) {
        this.own = own;
    }
}
