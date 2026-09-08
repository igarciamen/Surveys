package com.igarciamen.responses.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionSchemaDto {
    private Long id;
    private String type;
    private String label;
    private boolean required;
    private List<OptionSchemaDto> options = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public List<OptionSchemaDto> getOptions() { return options; }
    public void setOptions(List<OptionSchemaDto> options) { this.options = options; }
}
