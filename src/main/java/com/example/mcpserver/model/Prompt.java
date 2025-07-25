package com.example.mcpserver.model;

import java.util.List;

public class Prompt {
    private String name;
    private String description;
    private List<PromptArgument> arguments;

    public Prompt() {
    }

    public Prompt(String name, String description, List<PromptArgument> arguments) {
        this.name = name;
        this.description = description;
        this.arguments = arguments;
    }

    // Getters and setters
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

    public List<PromptArgument> getArguments() {
        return arguments;
    }

    public void setArguments(List<PromptArgument> arguments) {
        this.arguments = arguments;
    }

    public static class PromptArgument {
        private String name;
        private String description;
        private boolean required;

        public PromptArgument() {
        }

        public PromptArgument(String name, String description, boolean required) {
            this.name = name;
            this.description = description;
            this.required = required;
        }

        // Getters and setters
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

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
