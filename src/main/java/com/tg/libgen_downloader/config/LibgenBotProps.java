package com.tg.libgen_downloader.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LibgenBotProps {
    private String token,
            username,
            non_science_url,
            file_catalog,
            fiction_url,
            article_url,
            non_science_first,
            non_science_second,
            non_science_and_fiction_final,
            fiction_first,
            article_first,
            article_second;

    public LibgenBotProps(String token,
                          String username,
                          String non_science_url,
                          String file_catalog,
                          String fiction_url,
                          String article_url,
                          String non_science_first,
                          String non_science_second,
                          String non_science_and_fiction_final,
                          String fiction_first,
                          String article_first,
                          String article_second) {
        this.token = token;
        this.username = username;
        this.non_science_url = non_science_url;
        this.file_catalog = file_catalog;
        this.fiction_url = fiction_url;
        this.article_url = article_url;
        this.non_science_first = non_science_first;
        this.non_science_second = non_science_second;
        this.non_science_and_fiction_final = non_science_and_fiction_final;
        this.fiction_first = fiction_first;
        this.article_first = article_first;
        this.article_second = article_second;
    }
}
