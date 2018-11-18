package com.elon.hystrix.controller;

import com.elon.hystrix.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    @Autowired
    private BookService bookService;

    @RequestMapping("/to-read")
    public String readingList() {
        return bookService.readingList();
    }
}
