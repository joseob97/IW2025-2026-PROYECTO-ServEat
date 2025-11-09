package com.serveat.view.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;

public class MainLayout extends AppLayout {
    public MainLayout() {
        addToNavbar(new H1("ServEat"));
    }
}