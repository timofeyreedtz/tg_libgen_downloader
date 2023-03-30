package com.tg.libgen_downloader.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:/bot.properties")
public class LibgenBotConfig {
    @Autowired
    private Environment env;
    @Bean
    public LibgenBotProps getProperties(){
        return new LibgenBotProps(env.getProperty("token"), env.getProperty("username")
                ,env.getProperty("non_science_url")
                ,env.getProperty("file_catalog")
                ,env.getProperty("fiction_url")
                ,env.getProperty("article_url")
                ,env.getProperty("non_science_first")
                ,env.getProperty("non_science_second")
                ,env.getProperty("non_science_and_fiction_final")
                ,env.getProperty("fiction_first")
                ,env.getProperty("article_first")
                ,env.getProperty("article_second"));
    }
}
