package shareyourstory.domain.event.controller;

import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.event.model.Event;
import shareyourstory.domain.event.service.EventService;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.WebSocketService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class EventController {

    @Autowired
    EventService eventService;

    @Autowired
    WebSocketService webSocketService;

    @GetMapping("/api/events")
    public List<Event> getEvents(@AuthenticationPrincipal User user) {
        return eventService.getAllEvents(user);
    }

    @GetMapping("/api/events/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        Event event = eventService.getEventById(id, user);
        return event != null ? ResponseEntity.ok(event) : ResponseEntity.notFound().build();
    }
    
    @PostMapping("/api/events")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event createdEvent = eventService.createEvent(event);
        webSocketService.broadcastEventChange("CREATE", createdEvent);
        return ResponseEntity.ok(createdEvent);
    }

    @GetMapping("/api/events/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable Integer id) {
        Event event = eventService.getEventById(id);
        return event == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(event);
    }

    @PostMapping("/api/events/{id}/interest")
    public ResponseEntity<Event> addInterest(@PathVariable Integer id) {
        Event event = eventService.addInterest(id);
        return event == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(event);
    }

    @DeleteMapping("/api/events/{id}/interest")
    public ResponseEntity<Event> removeInterest(@PathVariable Integer id) {
        Event event = eventService.removeInterest(id);
        return event == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(event);
    }


    @PutMapping("/api/events/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Integer id, @RequestBody Event event) {
        Event updatedEvent = eventService.updateEvent(id, event);
        if (updatedEvent != null) {
            webSocketService.broadcastEventChange("UPDATE", updatedEvent);
            return ResponseEntity.ok(updatedEvent);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/api/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Integer id) {
        Event event = eventService.getEventById(id);
        if (event != null) {
            eventService.deleteEvent(id);
            webSocketService.broadcastEventChange("DELETE", event);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
