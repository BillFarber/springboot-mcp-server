package com.example.mcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class McpRequest {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    private Object id; // Changed from String to Object
    private String method;
    private Object params;

    public McpRequest() {
    }

    public McpRequest(Object id, String method, Object params) { // Changed parameter type
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

    public Object getId() { // Changed return type
        return id;
    }

    public void setId(Object id) { // Changed parameter type
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
