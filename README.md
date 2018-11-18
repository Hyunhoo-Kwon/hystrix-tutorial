# hystrix-tutorial

This tutorial will use:
 - Java8
 - Spring Boot 2.1.0
 - Hystrix, the Netflix Hystrix fault tolerance library
 - Gradle

## Circuit Breaker
![Circuit Breaker](https://martinfowler.com/bliki/images/circuitBreaker/sketch.png)

시스템 간에 원격 호출을 하는 경우 장애 전파를 방지하기 위해 [Circuit Breaker pattern](https://martinfowler.com/bliki/CircuitBreaker.html)을 이용합니다. supplier 서버의 장애로 항상 timeout이 발생하는 경우, supplier 서버를 호출한 client 서버는 timeout이 발생할때 까지 응답이 밀리게 됩니다. 응답이 밀리는 동안 요청이 계속 쌓여 client 서버의 리소스가 부족해지며 최악의 경우 여러 시스템에서 연속적으로 장애가 발생할 수 있습니다. Circuit Breaker의 기본 원리는 원격 호출을 하는 함수를 래핑하여 오류를 모니터링하고, 장애가 특정 임계치를 넘어가면 추가 호출을 하는 대신 fallback 응답을 반환합니다.
