package com.elon.hystrix.controller;

import com.elon.hystrix.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rx.Single;

import java.util.concurrent.ExecutionException;

@RestController
public class BookController {

    @Autowired
    private BookService bookService;

    @RequestMapping("/to-read-sync")
    public String readingListSync() {
        return bookService.readingListSync();
    }

    @RequestMapping("/to-read-async")
    public String readingListAsync() throws ExecutionException, InterruptedException {
        return bookService.readingListAsync().get();
    }

    @RequestMapping("/to-read-reactive")
    public Single<String> readingListReactive() {
        return bookService.readingListReactive().toSingle();
    }
}
