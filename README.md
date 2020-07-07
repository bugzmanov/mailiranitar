## Mailiranitar

![image](https://user-images.githubusercontent.com/502482/86857154-e6991280-c08b-11ea-926a-9da32ef37599.png)

Just like mailinator but with pokemons!

## Problem analysis


## Protocol specification

`GET /v1/<key>`

Response content type: `application/octet-stream`

| Status Code   | Description           | 
| ------------- |:-------------:| 
| 200           | Successfull operation | 
| 404           | No data for that key      | 
| 500           | Service error        | 


## Running instructions


## Overall description

* Runtime & Language: scala on JVM 
* Webserver: Finatra (https://twitter.github.io/finatra/)
* Caching library: Caffeine (https://github.com/ben-manes/caffeine)
* Performance tests: locust (https://locust.io/)

## Service design 

Mailbox design:
   - to get by id: Map
   - to iterate LinkedList
   

Constraints:
- Avoiding heavy hitters and data skew - uniforming the data:
    - creation rate limit per account
    - max message size limit
    - max messages per account
    
- Expiration:
   - by acc creation date  - do we need cool-off period using tomb stones?
   - Memory pressure (drop oldest)
   - weak references drop - degraded case || auto-restart

https://www.techempower.com/benchmarks/#section=data-r15&hw=ph&test=fortune

## Load testing

https://github.com/bugzmanov/mailiranitar/blob/master/locust/locustfile.py

## Implementation plan

0. Design
1. Core
2. Rest api
3. Monitoring
