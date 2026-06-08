package shareyourstory.domain.storyMap.controller;

import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import shareyourstory.domain.storyMap.dto.NewStoryMapRequest;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.service.StoryMapService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;

@RestController
public class StoryMapController {

    @Autowired
    StoryMapService storyMapService;

    @GetMapping("/api/stories")
    public List<StoryMap> getAllStoryMapPoints() {
        return storyMapService.getAllStoryMaps();
    }

    @PostMapping("/api/stories")
    public ResponseEntity<StoryMap> createStoryMap(@Valid @RequestBody NewStoryMapRequest newStoryMapRequest) {
        StoryMap created = storyMapService.createStoryMap(newStoryMapRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Borrado directo de una historia por un moderador/administrador. */
    @DeleteMapping("/api/stories/{id}")
    public ResponseEntity<Void> deleteStoryMap(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        if (user == null
                || (user.getRole() != UserRole.PROFESSIONAL && user.getRole() != UserRole.ADMINISTRATOR)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean deleted = storyMapService.deleteStory(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
