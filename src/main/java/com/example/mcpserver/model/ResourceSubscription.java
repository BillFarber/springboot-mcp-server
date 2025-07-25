package com.example.mcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for resource subscription information
 */
public class ResourceSubscription {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("subscriptionId")
    private String subscriptionId;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("createdAt")
    private long createdAt;

    public ResourceSubscription() {
        this.createdAt = System.currentTimeMillis();
        this.active = true;
    }

    public ResourceSubscription(String uri, String clientId, String subscriptionId) {
        this();
        this.uri = uri;
        this.clientId = clientId;
        this.subscriptionId = subscriptionId;
    }

    // Getters and setters
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ResourceSubscription{" +
                "uri='" + uri + '\'' +
                ", clientId='" + clientId + '\'' +
                ", subscriptionId='" + subscriptionId + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
