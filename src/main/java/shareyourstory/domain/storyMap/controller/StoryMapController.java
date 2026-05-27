package shareyourstory.domain.storyMap.controller;

import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import shareyourstory.domain.storyMap.dto.NewStoryMapRequest;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.service.StoryMapService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class StoryMapController {

    @Autowired
    StoryMapService storyMapService;

    @GetMapping("/api/stories")
    public List<StoryMap> getAllStoryMapPoints(@RequestParam String param) {
        return storyMapService.getAllStoryMaps();
    }

    @PostMapping("/api/stories")
    public int createStoryMap(@RequestBody NewStoryMapRequest newStoryMapRequest,
            HttpServletRequest request) {
        return storyMapService.createStoryMap(newStoryMapRequest);
    }
}
