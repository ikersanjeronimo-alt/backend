package shareyourstory.domain.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

// Respuesta de texto libre de un usuario a un cuestionario TEXT. Restriccion unica
// (form_id, user_id): UNA respuesta por cuenta.
@Entity
@Table(name = "event_form_replies",
       uniqueConstraints = @UniqueConstraint(columnNames = { "form_id", "user_id" }))
public class EventFormReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "reply_text", nullable = false, length = 500)
    private String text;

    private LocalDateTime createdAt;

    public EventFormReply() {}

    public EventFormReply(Long formId, Integer userId, String text) {
        this.formId = formId;
        this.userId = userId;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
