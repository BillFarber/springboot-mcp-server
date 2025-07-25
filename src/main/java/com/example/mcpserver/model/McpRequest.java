package com.example.mcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class McpRequest {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    private String id;
    private String method;
    private Object params;

    public McpRequest() {
    }

    public McpRequest(String id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    // Getters and setters
    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
