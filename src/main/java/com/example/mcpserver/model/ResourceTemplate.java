package com.example.mcpserver.model;

public class ResourceTemplate {
    private String uriTemplate;
    private String name;
    private String description;
    private String mimeType;

    public ResourceTemplate() {
    }

    public ResourceTemplate(String uriTemplate, String name, String description, String mimeType) {
        this.uriTemplate = uriTemplate;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
    }

    // Getters and setters
    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
