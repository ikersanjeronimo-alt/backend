package shareyourstory.domain.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

// Voto de un usuario a una opcion de un cuestionario CHOICE. La restriccion unica
// (form_id, user_id) garantiza UN voto por cuenta (incluida la anonima, que tiene
// userId estable). El recuento por opcion se deriva contando estas filas.
@Entity
@Table(name = "event_form_votes",
       uniqueConstraints = @UniqueConstraint(columnNames = { "form_id", "user_id" }))
public class EventFormVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "option_index", nullable = false)
    private int optionIndex;

    private LocalDateTime createdAt;

    public EventFormVote() {}

    public EventFormVote(Long formId, Integer userId, int optionIndex) {
        this.formId = formId;
        this.userId = userId;
        this.optionIndex = optionIndex;
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

    public int getOptionIndex() {
        return optionIndex;
    }

    public void setOptionIndex(int optionIndex) {
        this.optionIndex = optionIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
