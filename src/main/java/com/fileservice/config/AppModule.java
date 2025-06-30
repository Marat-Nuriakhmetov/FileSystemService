package com.fileservice.config;

import com.fileservice.service.FileCreateService;
import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
       bind(FileCreateService.class);
    }
}