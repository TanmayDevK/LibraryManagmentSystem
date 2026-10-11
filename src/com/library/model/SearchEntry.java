package com.library.model;

import java.time.LocalDateTime;

public class SearchEntry {
    private final String keyword;
    private final LocalDateTime searchedAt;

    public SearchEntry(String keyword, LocalDateTime searchedAt) {
        this.keyword = keyword;
        this.searchedAt = searchedAt;
    }

    public String getKeyword() { return keyword; }
    public LocalDateTime getSearchedAt() { return searchedAt; }
}
