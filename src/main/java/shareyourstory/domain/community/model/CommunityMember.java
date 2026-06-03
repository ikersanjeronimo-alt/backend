package shareyourstory.domain.community.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;

/**
 * Pertenencia de un usuario a una comunidad (relacion N:M materializada).
 * El par (userId, communityId) es unico para no duplicar membresias.
 */
@Entity
@Table(name = "community_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "communityId"}))
public class CommunityMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Long communityId;

    public CommunityMember() {}

    public CommunityMember(Integer userId, Long communityId) {
        this.userId = userId;
        this.communityId = communityId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }
}
