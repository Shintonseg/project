package com.tible.ocm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class SrnArticles<A> {

    private String version;
    private LocalDateTime dateTime;
    private List<A> articles;
    private Integer total;

    public SrnArticles(String version, List<A> articles) {
        this.version = version;
        this.dateTime = LocalDateTime.now();
        this.articles = articles;
        this.total = articles.size();
    }
}
