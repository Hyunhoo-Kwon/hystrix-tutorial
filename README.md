# hystrix-tutorial

This tutorial will use:
 - Java8
 - Spring Boot 2.1.0
 - Hystrix, the Netflix Hystrix fault tolerance library
 - Gradle

## Circuit Breaker
![Circuit Breaker](https://martinfowler.com/bliki/images/circuitBreaker/sketch.png)

시스템 간에 원격 호출을 하는 경우 장애 전파를 방지하기 위해 [Circuit Breaker pattern](https://martinfowler.com/bliki/CircuitBreaker.html)을 이용합니다. supplier 서버의 장애로 항상 timeout이 발생하는 경우, supplier 서버를 호출한 client 서버는 timeout이 발생할때 까지 응답이 밀리게 됩니다. 응답이 밀리는 동안 요청이 계속 쌓여 client 서버의 리소스가 부족해지며 최악의 경우 여러 시스템에서 연속적으로 장애가 발생할 수 있습니다. Circuit Breaker의 기본 원리는 원격 호출을 하는 함수를 래핑하여 오류를 모니터링하고, 장애가 특정 임계치를 넘어가면 추가 호출을 하는 대신 fallback 응답을 반환합니다.

## Hystric 적용 예제
### Supplier 서버 구현
 > https://github.com/Hyunhoo-Kwon/simple-api-server
 
### Client 서버 구현 + Hystrix 적용
#### 1. 프로젝트 환경설정
 1. [스프링 부트 프로젝트 생성](https://start.spring.io/) 옵션:
    - Gradle Project
    - Java
    - As dependencies:
      - Web
 2. [build.gradle](https://github.com/Hyunhoo-Kwon/hystrix-tutorial/blob/master/build.gradle) 파일에 디펜던시 추가:
    - [spring-cloud-starter-netflix-hystrix](https://github.com/spring-cloud/spring-cloud-netflix/tree/master/spring-cloud-starter-netflix/spring-cloud-starter-netflix-hystrix)
    ```
    dependencies {
     ...
     compile('org.springframework.cloud:spring-cloud-starter-netflix-hystrix:2.0.2.RELEASE')
    }
    ```
#### 2. API 호출 구현 및 Hystrix 적용
 1. HystrixApplication.java: 
    - @EnableCircuitBreaker 추가
 ```
 @SpringBootApplication
 @EnableCircuitBreaker
 public class HystrixApplication {

  public static void main(String[] args) {
   SpringApplication.run(HystrixApplication.class, args);
  }
 }
 ```
 2. BookController.java: 
    - @ResquestMapping 설정
 ```
 @RestController
 public class BookController {
    @Autowired
    private BookService bookService;

    @RequestMapping("/to-read-sync")
    public String readingListSync() {
        return bookService.readingListSync();
    }
 }
 ```
 3. BookService.java: 
    - API를 호출하는 readingListSync 메소드에 Hystrix 적용
    - 장애 상황에 fallbackMethod로 reliable 메소드 수행
 ```
 @Service
 public class BookService {
    private static final String URL = "http://localhost:8090/recommended";
    private final RestTemplate restTemplate = new RestTemplate();

    @HystrixCommand(fallbackMethod = "reliable",
            commandProperties = {@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500")})
    public String readingListSync() {
        return this.restTemplate.getForObject(URL, String.class);
    }

    public String reliable() {
        return "Cloud Native Java (O'Reilly)";
    }
 }
 ```
 > Hystrix는 @HystrixCommand 어노테이션을 적용한 모든 메소드를 찾고, Hystrix가 이를 모니터링 할 수 있도록 circuit breaker에 연결된 프록시에 해당 메소드를 래핑합니다. @Component 또는 @Service를 적용한 클래스에서만 동작합니다.

## Hystrix 동작 방식
### [Flow Chart](https://github.com/Netflix/Hystrix/wiki/How-it-Works)
![Flow Chart](https://raw.githubusercontent.com/wiki/Netflix/Hystrix/images/hystrix-command-flow-chart.png)
 1. HystrixCommand 객체 생성
 ```
 @HystrixCommand
 ```
 2. 동기식/비동기식 Command 실행
    - Synchronous: .execute()
    ```
    @HystrixCommand(fallbackMethod = "reliable")
    public String readingList() {
        return this.restTemplate.getForObject(URL, String.class);
    }
    ```
    - Asynchronous: .queue()
    > AsyncResult 반환
    ```
    @HystrixCommand(fallbackMethod = "reliable")
    public Future<String> readingList() {
        return new AsyncResult<String>() {
            @Override
            public String invoke() {
                return restTemplate.getForObject(URL, String.class);
            }
        };
    }
    ```
    - Reactive: .observe() / .toObservable()
    > Observable 반환
    ```
    // observe()
    @HystrixCommand(fallbackMethod = "reliable", observableExecutionMode = EAGER)
    public Observable<String> readingListReactive() {
        return Observable.just(restTemplate.getForObject(URL, String.class));
    }
    
    // toObservable
    @HystrixCommand(fallbackMethod = "reliable", observableExecutionMode = LAZY)
    ...
    ```
    
## 참고
 1. Hystrix documentation: https://github.com/Netflix/hystrix/wiki
 2. Hystrix tutorial: https://spring.io/guides/gs/circuit-breaker/
