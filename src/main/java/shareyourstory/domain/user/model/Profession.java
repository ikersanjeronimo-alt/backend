package shareyourstory.domain.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "professions")
public class Profession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "professionId")
    private Integer professionId;

    @Column(name = "professionDescription", nullable = false, length = 100, unique = true)
    private String professionDescription;

    public Profession() {}

    public Integer getProfessionId() {
        return professionId;
    }

    public void setProfessionId(Integer professionId) {
        this.professionId = professionId;
    }

    public String getProfessionDescription() {
        return professionDescription;
    }

    public void setProfessionDescription(String professionDescription) {
        this.professionDescription = professionDescription;
    }
}
