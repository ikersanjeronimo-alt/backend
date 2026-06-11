package shareyourstory.domain.event.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Cuestionario embebido en un evento. UNO por evento (event_id unico): el editor
// del front asume un solo formulario por evento. Las opciones (solo CHOICE) se
// guardan en una tabla hija ordenada (event_form_options) preservando el orden.
@Entity
@Table(name = "event_forms", uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class EventForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private Integer eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormKind kind;

    @Column(nullable = false, length = 140)
    private String question;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_form_options", joinColumns = @JoinColumn(name = "form_id"))
    @OrderColumn(name = "opt_index")
    @Column(name = "opt_text", length = 80)
    private List<String> options = new ArrayList<>();

    private LocalDateTime createdAt;

    public EventForm() {}

    public EventForm(Integer eventId, FormKind kind, String question, List<String> options) {
        this.eventId = eventId;
        this.kind = kind;
        this.question = question;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public FormKind getKind() {
        return kind;
    }

    public void setKind(FormKind kind) {
        this.kind = kind;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
