package com.igarciamen.statistics.client.dto;

public class OptionCountView {
    private Long optionId;
    private String label;
    private long count;

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
