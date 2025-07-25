package com.example.mcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for resource update notifications sent to subscribed clients
 */
public class ResourceNotification {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("method")
    private String method = "notifications/resources/updated";

    @JsonProperty("params")
    private NotificationParams params;

    public ResourceNotification() {
    }

    public ResourceNotification(String uri, String subscriptionId) {
        this.params = new NotificationParams(uri, subscriptionId);
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public NotificationParams getParams() {
        return params;
    }

    public void setParams(NotificationParams params) {
        this.params = params;
    }

    public static class NotificationParams {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("subscriptionId")
        private String subscriptionId;

        @JsonProperty("timestamp")
        private long timestamp;

        public NotificationParams() {
            this.timestamp = System.currentTimeMillis();
        }

        public NotificationParams(String uri, String subscriptionId) {
            this();
            this.uri = uri;
            this.subscriptionId = subscriptionId;
        }

        // Getters and setters
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public void setSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
