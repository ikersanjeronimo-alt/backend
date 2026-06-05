package shareyourstory.domain.user.model;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`userId`")
    private Integer userId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "`lastName`", length = 255)
    private String lastName;

    @Column(name = "`userName`", nullable = false, unique = true, length = 255)
    private String userName;

    @Column(name = "`nickName`", nullable = false, unique = true, length = 255)
    private String nickName;

    @Column(name = "`userPassword`", nullable = false, length = 255)
    private String userPassword;

    @Column(name = "`companyName`", length = 255)
    private String companyName;

    @Column(name = "mail", unique = true, length = 255)
    private String mail;

    @Column(name = "`creationDate`", nullable = false)
    private LocalDate creationDate;

    @Column(name = "profession", length = 255)
    private String profession;

    @Column(name = "specialization", length = 255)
    private String specialization;

    @ManyToOne
    @JoinColumn(name = "professionId")
    private Profession professionRef;

    @ManyToOne
    @JoinColumn(name = "specializationId")
    private Specialization specializationRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role = UserRole.ANON;

    @Column(name = "`secretKey`", unique = true)
    private String secretKey;

    @Column(name = "`twoFactorEnabled`", nullable = false)
    private boolean twoFactorEnabled = false;

    // Temas elegidos en el onboarding, separados por comas (CSV simple).
    @Column(name = "topics", length = 512)
    private String topics;

    // Moderacion de miembros.
    @Column(name = "warnings", nullable = false)
    private int warnings = 0;

    @Column(name = "banned", nullable = false)
    private boolean banned = false;

    public User() {}

    @PrePersist
    public void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDate.now();
        }
        if (role == null) {
            role = UserRole.ANON;
        }
        if (nickName == null) {
            nickName = userName;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getId() {
        return userId;
    }

    public void setId(Integer id) {
        this.userId = id;
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public String getPassword() {
        return userPassword;
    }

    public void setPassword(String password) {
        this.userPassword = password;
    }

    public String getEmail() {
        return mail;
    }

    public void setEmail(String email) {
        this.mail = email;
    }

    public String getCompany() {
        return companyName;
    }

    public void setCompany(String company) {
        this.companyName = company;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Profession getProfessionRef() {
        return professionRef;
    }

    public void setProfessionRef(Profession professionRef) {
        this.professionRef = professionRef;
    }

    public Specialization getSpecializationRef() {
        return specializationRef;
    }

    public void setSpecializationRef(Specialization specializationRef) {
        this.specializationRef = specializationRef;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}
