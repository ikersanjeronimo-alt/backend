package shareyourstory.domain.storyMap.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.storyMap.dto.NewStoryMapRequest;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.repository.StoryMapRepository;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class StoryMapService {

    @Autowired
    StoryMapRepository storyMapRepository;

    @Autowired
    WebSocketService webSocketService;

    public List<StoryMap> getAllStoryMaps() {
        return storyMapRepository.findAll();
    }

    public StoryMap createStoryMap(NewStoryMapRequest newStoryMapRequest) {
        StoryMap newStoryMap = new StoryMap();
        newStoryMap.setMessage(newStoryMapRequest.text());
        newStoryMap.setLatitude(newStoryMapRequest.lat());
        newStoryMap.setLongitude(newStoryMapRequest.lng());

        StoryMap saved = storyMapRepository.save(newStoryMap);
        webSocketService.broadcastNewStoryMap(saved);
        return saved;
    }
}
