package me.nexuswarp.manager;

import java.util.UUID;

public class TpaRequest {

    private final UUID requesterId;
    private final String requesterName;
    private final UUID targetId;
    private final String targetName;
    private final long creationTime;

    public TpaRequest(UUID requesterId, String requesterName, UUID targetId, String targetName) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.creationTime = System.currentTimeMillis();
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isExpired(int timeoutSeconds) {
        if (timeoutSeconds <= 0) return false;
        return (System.currentTimeMillis() - creationTime) > (timeoutSeconds * 1000L);
    }
}
