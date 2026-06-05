package shareyourstory.websocket.service;

import shareyourstory.domain.storyMap.model.StoryMap;

/**
 * Evento de historia del mapa difundido por /topic/storyMap. Lleva una accion
 * ("CREATE" | "DELETE") para que el cliente sepa si debe anadir el punto o
 * quitarlo. En DELETE solo es relevante el id.
 */
public record StoryMapEventDTO(
        String action,
        Integer id,
        String message,
        double latitude,
        double longitude) {

    public static StoryMapEventDTO created(StoryMap story) {
        return new StoryMapEventDTO(
                "CREATE", story.getId(), story.getMessage(),
                story.getLatitude(), story.getLongitude());
    }

    public static StoryMapEventDTO deleted(Integer storyId) {
        return new StoryMapEventDTO("DELETE", storyId, null, 0, 0);
    }
}
