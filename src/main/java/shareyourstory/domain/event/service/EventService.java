package shareyourstory.domain.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.event.model.EventInterest;
import shareyourstory.domain.event.model.Event;
import shareyourstory.domain.event.repository.EventInterestRepository;
import shareyourstory.domain.event.repository.EventRepository;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class EventService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventInterestRepository eventInterestRepository;

    @Autowired
    WebSocketService webSocketService;

    public List<Event> getAllEvents() {
        return getAllEvents(null);
    }

    public List<Event> getAllEvents(User user) {
        return eventRepository.findAll().stream()
            .map(event -> hydrateInterestState(event, user))
            .toList();
    }

    public Event getEventById(Integer id) {
        return getEventById(id, null);
    }

    public Event getEventById(Integer id, User user) {
        Optional<Event> event = eventRepository.findById(id);
        return event.map(value -> hydrateInterestState(value, user)).orElse(null);
    }

    public Event markInterest(Integer id, User user) {
        User actor = requireRegisteredUser(user);
        Event event = requireEvent(id);
        if (!eventInterestRepository.existsByEvent_IdAndUser_UserId(id, actor.getUserId())) {
            eventInterestRepository.save(new EventInterest(event, actor));
        }
        return saveCountAndBroadcast(event, actor);
    }

    public Event unmarkInterest(Integer id, User user) {
        User actor = requireRegisteredUser(user);
        Event event = requireEvent(id);
        if (eventInterestRepository.existsByEvent_IdAndUser_UserId(id, actor.getUserId())) {
            eventInterestRepository.deleteByEvent_IdAndUser_UserId(id, actor.getUserId());
        }
        return saveCountAndBroadcast(event, actor);
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
            return hydrateInterestState(eventRepository.save(event), null);
        }
        return null;
    }

    @Transactional
    public void deleteEvent(Integer id) {
        eventInterestRepository.deleteByEvent_Id(id);
        eventRepository.deleteById(id);
    }

    private Event saveCountAndBroadcast(Event event, User user) {
        event.setReaction((int) eventInterestRepository.countByEvent_Id(event.getId()));
        Event saved = eventRepository.save(event);
        Event hydrated = hydrateInterestState(saved, user);
        boolean interested = hydrated.isInterested();
        hydrated.setInterested(false);
        webSocketService.broadcastEventChange("UPDATE", hydrated);
        hydrated.setInterested(interested);
        return hydrated;
    }

    private Event hydrateInterestState(Event event, User user) {
        event.setReaction((int) eventInterestRepository.countByEvent_Id(event.getId()));
        event.setInterested(canUseInterests(user)
            && eventInterestRepository.existsByEvent_IdAndUser_UserId(event.getId(), user.getUserId()));
        return event;
    }

    private Event requireEvent(Integer id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private User requireRegisteredUser(User user) {
        if (!canUseInterests(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registered username required");
        }
        return user;
    }

    private boolean canUseInterests(User user) {
        return user != null
            && user.getUserId() != null
            && user.getRole() != UserRole.ANON;
    }
}
