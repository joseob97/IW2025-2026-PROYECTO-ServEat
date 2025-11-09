package com.serveat;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme("serveat") // 👈 usa el tema ubicado en frontend/themes/serveat
public class ServEatApplication implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(ServEatApplication.class, args);
    }
}