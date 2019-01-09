package com.elon.hystrix.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.command.AsyncResult;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import rx.Observable;

import java.util.concurrent.Future;

import static com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode.EAGER;

@Service
public class BookService {

    private static final String URL = "http://localhost:8090/recommended";
    private final RestTemplate restTemplate = new RestTemplate();

    @HystrixCommand(fallbackMethod = "reliable",
            commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000"),
                @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "4"),
                @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")
            },
            threadPoolProperties = {
                @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "10000")
            })
    public String readingListSync() {
        return this.restTemplate.getForObject(URL, String.class);
    }

    @HystrixCommand(fallbackMethod = "reliable")
    public Future<String> readingListAsync() {
        return new AsyncResult<String>() {
            @Override
            public String invoke() {
                return restTemplate.getForObject(URL, String.class);
            }
        };
    }

    @HystrixCommand(fallbackMethod = "reliable", observableExecutionMode = EAGER,
            commandProperties = {@HystrixProperty(name = "execution.isolation.strategy", value = "SEMAPHORE")})
    public Observable<String> readingListReactive() {
        return Observable.just(restTemplate.getForObject(URL, String.class));
    }

    public Mono<String> readingListReactor() {
        return WebClient.create(URL).get()
                .retrieve()
                .bodyToMono(String.class)
                ;
    }

    public Mono<String> readingListReactorWrappedWithHystrix() {
        return HystrixCommands
                .from(readingListReactor())
                .fallback(Mono.just("webclient fallback"))
                .commandName("readingListReactor")
                .toMono();
    }

    public String reliable() {
        return "Cloud Native Java (O'Reilly)";
    }
}
