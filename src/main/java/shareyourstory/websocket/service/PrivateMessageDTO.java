package shareyourstory.websocket.service;

public class PrivateMessageDTO {
    private String id;
    private String from; // "user" or "professional"
    private String text;
    private String time;

    public PrivateMessageDTO() {}

    public PrivateMessageDTO(String id, String from, String text, String time) {
        this.id = id;
        this.from = from;
        this.text = text;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
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
}
