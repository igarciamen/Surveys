package com.igarciamen.responses.payloads.response;

public class OptionCountDto {
    private Long optionId;
    private String label;
    private long count;

    public OptionCountDto(Long optionId, String label, long count) {
        this.optionId = optionId;
        this.label = label;
        this.count = count;
    }
    public Long getOptionId() { return optionId; }
    public String getLabel() { return label; }
    public long getCount() { return count; }
}
