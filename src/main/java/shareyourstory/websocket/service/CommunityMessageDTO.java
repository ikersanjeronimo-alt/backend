package shareyourstory.websocket.service;

public class CommunityMessageDTO {
    private String id;
    private String username;
    private String text;
    private String time;
    private boolean own;
    // "DELETE" cuando el evento es un borrado; null para crear/actualizar.
    private String action;

    public CommunityMessageDTO() {}

    public CommunityMessageDTO(String id, String username, String text, String time, boolean own) {
        this.id = id;
        this.username = username;
        this.text = text;
        this.time = time;
        this.own = own;
    }

    /** Evento de borrado: el cliente lo identifica por action == "DELETE" + id. */
    public static CommunityMessageDTO deleted(String id) {
        CommunityMessageDTO dto = new CommunityMessageDTO();
        dto.setId(id);
        dto.setAction("DELETE");
        return dto;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
