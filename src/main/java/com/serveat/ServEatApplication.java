package com.serveat;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Theme("serveat")
@EnableCaching
@EnableScheduling
@EnableAsync
public class ServEatApplication implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(ServEatApplication.class, args);
    }
}