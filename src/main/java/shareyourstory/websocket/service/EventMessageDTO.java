package shareyourstory.websocket.service;

public class EventMessageDTO {
    private String action;
    private Object event;

    public EventMessageDTO() {}

    public EventMessageDTO(String action, Object event) {
        this.action = action;
        this.event = event;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }
}
