package shareyourstory.domain.community.dto;

import shareyourstory.domain.community.model.Community;

/**
 * Vista de comunidad para la API. Expone el id como String, la categoria en
 * minusculas (para que case con los filtros del front), y `joined`/`members`
 * calculados por usuario a partir de la membresia real. No vuelca la entidad JPA.
 */
public record CommunityResponse(
        String id,
        String emoji,
        String name,
        String mod,
        String modUserId,
        String desc,
        int members,
        int online,
        String category,
        boolean joined,
        String pinnedNote,
        boolean chatClosed) {

    public static CommunityResponse from(Community c, boolean joined, int members, int online) {
        return new CommunityResponse(
                String.valueOf(c.getId()),
                c.getEmoji(),
                c.getName(),
                c.getMod(),
                c.getModUserId() == null ? null : String.valueOf(c.getModUserId()),
                c.getDesc(),
                members,
                online,
                c.getCategory() == null ? null : c.getCategory().name().toLowerCase(),
                joined,
                c.getPinnedNote(),
                c.isChatClosed());
    }
}
