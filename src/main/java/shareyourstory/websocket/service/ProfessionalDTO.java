package shareyourstory.websocket.service;

import java.util.List;

public class ProfessionalDTO {
    private String id;
    private String name;
    private String specialty;
    private List<String> tags;
    private String availability;
    private String availableAt;
    private String bio;

    public ProfessionalDTO() {}

    public ProfessionalDTO(String id, String name, String specialty, List<String> tags,
            String availability, String availableAt, String bio) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.tags = tags;
        this.availability = availability;
        this.availableAt = availableAt;
        this.bio = bio;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(String availableAt) {
        this.availableAt = availableAt;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
