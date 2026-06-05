package shareyourstory.websocket.service;

public class PrivateMessageDTO {
    private String id;
    private String userId;
    private String professionalId;
    private String from; // "user" or "professional"
    private String text;
    private String time;
    // Ids de la conversacion: permiten al cliente enrutar el mensaje al hilo
    // correcto cuando llega por su cola personal (/user/queue/private).
    private String userId;
    private String professionalId;
    // "DELETE" cuando el evento es un borrado; null para un mensaje normal.
    private String action;

    public PrivateMessageDTO() {}

    public PrivateMessageDTO(String id, String from, String text, String time,
            String userId, String professionalId) {
        this.id = id;
        this.from = from;
        this.text = text;
        this.time = time;
        this.userId = userId;
        this.professionalId = professionalId;
    }

    /**
     * Evento de borrado: el cliente lo identifica por action == "DELETE" + id, y
     * usa userId/professionalId para enrutarlo al hilo correcto.
     */
    public static PrivateMessageDTO deleted(String id, String userId, String professionalId) {
        PrivateMessageDTO dto = new PrivateMessageDTO();
        dto.setId(id);
        dto.setAction("DELETE");
        dto.setUserId(userId);
        dto.setProfessionalId(professionalId);
        return dto;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PrivateMessageDTO(String id, String userId, String professionalId, String from, String text, String time) {
        this.id = id;
        this.userId = userId;
        this.professionalId = professionalId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfessionalId() {
        return professionalId;
    }

    public void setProfessionalId(String professionalId) {
        this.professionalId = professionalId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfessionalId() {
        return professionalId;
    }

    public void setProfessionalId(String professionalId) {
        this.professionalId = professionalId;
    }
}
