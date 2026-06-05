package shareyourstory.domain.storyMap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewStoryMapRequest(
        @NotBlank @Size(max = 300) String text,
        double lat,
        double lng) {
}
