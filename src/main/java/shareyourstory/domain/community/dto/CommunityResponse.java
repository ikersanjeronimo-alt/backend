package shareyourstory.domain.community.dto;

import shareyourstory.domain.community.model.Community;

/**
 * Vista de comunidad para la API. Expone el id como String (el front lo compara
 * con el parametro de ruta, que es String) y la categoria en minusculas (para
 * que case con los filtros del front). No vuelca la entidad JPA directamente.
 */
public record CommunityResponse(
        String id,
        String emoji,
        String name,
        String mod,
        String desc,
        int members,
        int online,
        String category,
        boolean joined,
        String pinnedNote) {

    public static CommunityResponse from(Community c) {
        return new CommunityResponse(
                String.valueOf(c.getId()),
                c.getEmoji(),
                c.getName(),
                c.getMod(),
                c.getDesc(),
                c.getMembers(),
                c.getOnline(),
                c.getCategory() == null ? null : c.getCategory().name().toLowerCase(),
                c.isJoined(),
                c.getPinnedNote());
    }
}
