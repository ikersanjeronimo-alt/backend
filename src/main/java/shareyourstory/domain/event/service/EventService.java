package shareyourstory.domain.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shareyourstory.domain.event.model.Event;
import shareyourstory.domain.event.repository.EventInterestRepository;
import shareyourstory.domain.event.repository.EventRepository;
import shareyourstory.domain.user.model.User;
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

    /** Registra interes del usuario y devuelve el evento actualizado. */
    @Transactional
    public Event addInterest(Integer id, User user) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) {
            return null;
        }
        if (user != null && user.getUserId() != null
                && !eventInterestRepository.existsByEvent_IdAndUser_UserId(id, user.getUserId())) {
            eventInterestRepository.save(new shareyourstory.domain.event.model.EventInterest(event, user));
        }
        Event hydrated = hydrateInterestState(event, user);
        webSocketService.broadcastEventChange("UPDATE", hydrated);
        return hydrated;
    }

    /** Quita el interes del usuario y devuelve el evento actualizado. */
    @Transactional
    public Event removeInterest(Integer id, User user) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) {
            return null;
        }
        if (user != null && user.getUserId() != null) {
            eventInterestRepository.deleteByEvent_IdAndUser_UserId(id, user.getUserId());
        }
        Event hydrated = hydrateInterestState(event, user);
        webSocketService.broadcastEventChange("UPDATE", hydrated);
        return hydrated;
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

    private Event hydrateInterestState(Event event, User user) {
        event.setReaction((int) eventInterestRepository.countByEvent_Id(event.getId()));
        event.setInterested(canUseInterests(user)
            && eventInterestRepository.existsByEvent_IdAndUser_UserId(event.getId(), user.getUserId()));
        return event;
    }

    private boolean canUseInterests(User user) {
        // Tambien los anonimos: tienen userId estable (el token anonimo se reutiliza
        // entre recargas), asi que su "me interesa" persiste igual que el de un USER.
        return user != null
            && user.getUserId() != null;
    }
}
