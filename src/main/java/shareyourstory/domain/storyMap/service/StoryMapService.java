package shareyourstory.domain.storyMap.service;

import java.util.List;
import org.apache.catalina.connector.Response;
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

    public int createStoryMap(NewStoryMapRequest newStoryMapRequest) {
        StoryMap newStoryMap = new StoryMap();

        newStoryMap.setMessage(newStoryMapRequest.text());
        newStoryMap.setLatitude(newStoryMapRequest.lat());
        newStoryMap.setLongitude(newStoryMapRequest.lng());

        System.out.println(newStoryMap.getMessage());
        System.out.println(newStoryMap.getLatitude());
        System.out.println(newStoryMap.getLongitude());

        try {
            storyMapRepository.save(newStoryMap);
            webSocketService.broadcastNewStoryMap(newStoryMap);
            return Response.SC_CREATED;
        } catch (Exception e) {
            return Response.SC_NOT_ACCEPTABLE;
        }
    }
}
