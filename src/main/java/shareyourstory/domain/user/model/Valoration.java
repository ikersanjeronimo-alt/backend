package shareyourstory.domain.user.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "valorations")
public class Valoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "valorationId")
    private Integer valorationId;

    @Column(name = "valorationPoint", nullable = false)
    private Integer valorationPoint;

    @Column(name = "valorationDate", nullable = false)
    private LocalDate valorationDate;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    public Valoration() {}

    public Integer getValorationId() {
        return valorationId;
    }

    public void setValorationId(Integer valorationId) {
        this.valorationId = valorationId;
    }

    public Integer getValorationPoint() {
        return valorationPoint;
    }

    public void setValorationPoint(Integer valorationPoint) {
        this.valorationPoint = valorationPoint;
    }

    public LocalDate getValorationDate() {
        return valorationDate;
    }

    public void setValorationDate(LocalDate valorationDate) {
        this.valorationDate = valorationDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
