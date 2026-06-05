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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
