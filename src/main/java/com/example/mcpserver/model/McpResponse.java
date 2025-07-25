package com.example.mcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class McpResponse {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    private Object id; // Changed from String to Object to handle both strings and integers
    private Object result;
    private McpError error;

    public McpResponse() {
    }

    public McpResponse(Object id, Object result) { // Changed parameter type
        this.id = id;
        this.result = result;
    }

    public McpResponse(Object id, McpError error) { // Changed parameter type
        this.id = id;
        this.error = error;
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }

    public static class McpError {
        private int code;
        private String message;
        private Object data;

        public McpError() {
        }

        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public McpError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        // Getters and setters
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
