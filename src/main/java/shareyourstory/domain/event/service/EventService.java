package shareyourstory.domain.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.event.model.Event;
import shareyourstory.domain.event.repository.EventRepository;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class EventService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    WebSocketService webSocketService;


    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Integer id) {
        Optional<Event> event = eventRepository.findById(id);
        return event.orElse(null);
    }

    public void toggleInterest(Integer id) {
        Optional<Event> event = eventRepository.findById(id);

        if (event.isPresent()) {
            event.get().setReaction(event.get().getReaction() + 1);
            eventRepository.save(event.get());
            webSocketService.broadcastEventChange("UPDATE", event.get());
        }
    }

    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    public Event updateEvent(Integer id, Event eventDetails) {
        Optional<Event> existingEvent = eventRepository.findById(id);
        if (existingEvent.isPresent()) {
            Event event = existingEvent.get();
            if (eventDetails.getTitle() != null) {
                event.setTitle(eventDetails.getTitle());
            }
            if (eventDetails.getDescription() != null) {
                event.setDescription(eventDetails.getDescription());
            }
            if (eventDetails.getDate() != null) {
                event.setDate(eventDetails.getDate());
            }
            if (eventDetails.getPlace() != null) {
                event.setPlace(eventDetails.getPlace());
            }
            if (eventDetails.getTopic() != null) {
                event.setTopic(eventDetails.getTopic());
            }
            event.setReaction(eventDetails.getReaction());
            return eventRepository.save(event);
        }
        return null;
    }

    public void deleteEvent(Integer id) {
        eventRepository.deleteById(id);
    }
}
