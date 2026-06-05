package shareyourstory.websocket.service;

import shareyourstory.domain.community.model.Community;

public class CommunityChangeDTO {

    private String action;
    private Community community;

    public CommunityChangeDTO(String action, Community community) {
        this.action = action;
        this.community = community;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community community) {
        this.community = community;
    }
}
