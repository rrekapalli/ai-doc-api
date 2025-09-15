package com.hidoc.api.web;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;

@ControllerAdvice
public class WebBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Trim all incoming String values and convert empty strings to null
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
