# hystrix-tutorial

This tutorial will use:
 - Java8
 - Spring Boot 2.1.0
 - Hystrix, the Netflix Hystrix fault tolerance library
 - Gradle

## Circuit Breaker
![Circuit Breaker](https://martinfowler.com/bliki/images/circuitBreaker/sketch.png)

시스템 간에 원격 호출을 하는 경우 장애 전파를 방지하기 위해 [Circuit Breaker pattern](https://martinfowler.com/bliki/CircuitBreaker.html)을 이용합니다. supplier 서버의 장애로 항상 timeout이 발생하는 경우, supplier 서버를 호출한 client 서버는 timeout이 발생할때 까지 응답이 밀리게 됩니다. 응답이 밀리는 동안 요청이 계속 쌓여 client 서버의 리소스가 부족해지며 최악의 경우 여러 시스템에서 연속적으로 장애가 발생할 수 있습니다. Circuit Breaker의 기본 원리는 원격 호출을 하는 함수를 래핑하여 오류를 모니터링하고, 장애가 특정 임계치를 넘어가면 서킷을 open 상태로 변경하고 추가 호출을 하는 대신 fallback 응답을 반환합니다.

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
 
#### 3. Hystrix 설정
 1. [execution.isolation.strategy](https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.strategy): HystrixCommand.run() 실행 시 격리 전략 설정
    - Thread: 서비스 호출을 별도의 스레드에서 실행. HystrixCommand 사용 시 권장
    - Semaphore: 서비스 호출을 위한 별도의 스레드 미생성. HystrixObservableCommand 사용 시 권장
    > Thread에서 실행 시 timeout 설정 가능. 호출량이 너무 많아 개별 스레드를 생성하는 오버헤드가 너무 많을 경우 semaphore 사용
 2. [execution.isolation.thread.timeoutInMilliseconds](https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.thread.timeoutInMilliseconds): Thread에서 실행 시 timeout 설정

## Hystrix 동작 방식
### [Flow Chart](https://github.com/Netflix/Hystrix/wiki/How-it-Works)
![Flow Chart](https://raw.githubusercontent.com/wiki/Netflix/Hystrix/images/hystrix-command-flow-chart.png)
 1. HystrixCommand / HystrixObervableCommand 객체 생성
    - HystrixCommand / HystrixObervableCommand
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
    - Reactive: [.observe() / .toObservable()](https://github.com/Netflix/Hystrix/wiki/How-To-Use#reactive-execution)
    > Observable 반환. observableExecutionMode로 모드 선택.
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
 3. 캐시를 사용하는가?
 4. 서킷이 열려있는가?
    - 서킷이 열려있으면 Hystrix는 command를 실행하지 않고 fallback을 실행합니다
 5. 스레드 풀/큐/세마포어가 full인가?
    - (스레드에서 실행 중인 경우) 스레드 풀과 큐가 가득차면 command를 실행하지 않고 fallback을 실행합니다
    - (세마포어 사용 시) 세마포어가 가득차면 command를 실행하지 않고 fallback을 실행합니다
 6. HystrixCommand.run()
 7. 서킷 상태 계산
    - Hystrix는 성공, 실패, 타임아웃 등을 Circuit Breaker에 보고하며 Circuit Breaker는 이를 통계내어 임계치를 넘어가면 서킷을 open(trip)상태로 변경합니다
    - trip 상태가 되면 해당 서킷이 헬스 체크를 통해 다시 닫힐 때까지 요청이 막힙니다
 8. Fallback
     - command 수행에 실패할 경우 fallback을 수행합니다
    
## 참고
 1. Hystrix documentation: https://github.com/Netflix/hystrix/wiki
 2. Hystrix Javanica documentation: https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-javanica
 3. Hystrix tutorial: https://spring.io/guides/gs/circuit-breaker/
