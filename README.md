## Disposable email address service

![image](https://user-images.githubusercontent.com/502482/86857650-f107dc00-c08c-11ea-94e4-8c578d88bc42.png)

<span>Just like _mailinator_ but with pokemons!</span>

## Problem analysis


## Protocol specification

* Create a new, random email address:

`POST /mailboxes` 


* Create a new message for a specific email address:

`POST /mailboxes/{email address}/messages` 


* Retrieve an index of messages sent to an email address, including sender, subject, and id, in recency order. Support cursor-based pagination through the index:

`GET /mailboxes/{email address}/messages` 

* Retrieve a specific message by id:

`GET /mailboxes/{email address}/messages/{message id}` 


* Delete a specific email address and any associated messages:

`DELETE /mailboxes/{email address}` 

* Delete a specific message by id:

`DELETE /mailboxes/{email address}/messages/{message id}` 


## Running instructions

Prerequisites:
* git
* make
* docker
* docker-compose

### Build and run

```
git clone https://github.com/bugzmanov/mailiranitar.git
cd mailiranitar
make run

```

### Run performance benchmark (in docker)

```
make run
make perftest
```


### Clean up

```
make clean
```

## Tech specification

* Runtime & Language: scala on JVM 
* Webserver: Finatra (https://twitter.github.io/finatra/)
* Caching library: Caffeine (https://github.com/ben-manes/caffeine)
* Performance tests: locust (https://locust.io/)

## Analysis and design 

From a general perspective, we are talking about in-memory storage that potentially has low contention:
* mailboxes are independent
* assuming real-world scenario, concurrent access to a mailbox is relatively infrequent

Because we are using in-memory data, ideally we would want to avoid locking (and serial access) as much as possible. We should try to utilize optimistic locking
(lock-free or wait-free) as retries would be less expensive than context switching (assuming low contention).

Three main components of the system:
* Rest API translation layer
* MailboxRegistry
* Individual mailbox

<img src="https://user-images.githubusercontent.com/502482/86950967-e26a0500-c11e-11ea-8d5f-414a65680d7a.png" width="40%" height="50%"/>


### Rest API Translation layer

The main constraint is that I have only 7 hrs to finish the task. 

The decision is to use finatra framework for this layer:
1. Finatra is relatively high on popular performance benchmarks (https://www.techempower.com/benchmarks/#section=data-r15&hw=ph&test=fortune
). 
Finatra is the topmost framework in the list that is:
    a) mature (lindy-effect)
    b) JVM based
    c) easy to get going 
    d) high-level (using raw netty would get better performance, but would be impossible for me to finish the task in 7 hours)
    
2. Finatra provides basic monitoring capabilities out of the box (ideally I would prefer to build a monitoring system from scratch using micrometer library, but 7 hrs)


### MailboxRegistry

MailboxRegistry is responsible for keeping track of existing mailboxes, managing their lifecycle, and providing access to individual mailboxes.
The structure that keeps track of mailboxes:
- needs to be concurrently read (access by key)
- concurrently modified (access by key)

Because we are expecting high contention accessing the registry, using a map with stripped locking seems to be a reasonable option. 

Potential candidates: 
* ConcurrentHashMap in jdk https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html
* Guava cache https://guava.dev/releases/21.0/api/docs/com/google/common/cache/Cache.html
* Caffeine cache https://github.com/ben-manes/caffeine
* ConcurrentLinkedHashmap: https://github.com/ben-manes/concurrentlinkedhashmap/

From a benchmarking perspective (https://github.com/ben-manes/caffeine/wiki/Benchmarks) caffeine has the best throughput performance.
(its design proposed significant improvements upon the idea of striped locking: https://github.com/ben-manes/caffeine/wiki/Design )

Design decisions/shortcuts:
* Each mailbox would expire based on TTL from time of creation or Window TinyLfu in case of reaching max mailbox limit
* Statically defined the maximum allowed live mailboxes (see "Handling Memory Pressure" section)
* Expiration policy is mailbox based (in contrast to mail messages based) as it requires less bookkeeping

### Mailbox
Mailboxes are independent of each other from an API perspective, which implies no contention for accessing different boxes.

Access patterns:
- read by key
- random write - delete by key
- read by key and then scan
- write at predefined position - add a new message

The ideal data structure for this pattern would be doubly linked list with a hash table, to keep all access patterns under amortized O(1) 
(or O(page size) for pagination).

<img src="https://user-images.githubusercontent.com/502482/86956273-d2eeba00-c126-11ea-8bce-669d78bfab1d.png" width="50%" height="50%"/>

Potential candidates:
* ~~LinkedHashMap~~ - not thread-safe, do not expose underlying linked list
* ~~[ConcurrentLinkedHashMap](https://github.com/ben-manes/concurrentlinkedhashmap)~~  - do not expose underlying linked list
* Build custom implementation. 

I went with creating a custom data structure (mix of linkedlist and map) that can guard concurrent access via [StampedLock](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/StampedLock.html), 
we are expecting a low amount of write contention and can utilize an optimistic locking approach with retries. 

### Handling memory pressure

Two general approaches of handling memory pressure:
* Dynamically memory control
* Statically defined memory control


In dynamically control the system actively monitors itself and makes a decision to evict data when starts running out fo memory.
Benefits:
* More efficient memory utilization
* Less burden of software configuration and management

Drawbacks:
* More complex
* Actively consumes CPU resources
* Potentially introduces contention for data access

In a static control - software operator manually defines limits, which are enforced in the runtime.
Overall static control is expected to have better performance (and it's also possible to implement it within 7hrs)

To have full statically memory control, it sufficient to define the following limits:
* Max number of live mailboxes the system can support
* Max number of messages a single mailbox can have
* Max size of a message allowed (I did not have time to implement this constraint)

Max number of messages limit is implemented in FIFO fashion (the oldest messages got dropped first)
Max number of mailboxes liit is implemented in Window TinyLfu fashion (https://github.com/ben-manes/caffeine/wiki/Efficiency)

## Load testing

https://github.com/bugzmanov/mailiranitar/blob/master/locust/locustfile.py

To run perf benchmark against running mailiranitar service:

```
locust -f locust/locustfile.py --headless -u 3000 -r 50 -H http://<host>:8888
```


The test is running 300 (in this case) concurrent users. Each one of them:

1. Either creates a new mailbox (with 0.3 chance) or uses an existing one
2. Posts 6 new messages
3. Reads a random message
4. Reads a page of messages
5. Removes a random message
6. With p=0.1 deletes the mailbox

Benchmarking results on my MacBook Pro 2013 (using docker):

```
Type                 Name                                                           # reqs    50%    66%    75%    80%    90%    95%    98%    99%  99.9% 99.99%   100%
------------------------------------------------------------------------------------------------------------------------------------------------------
 POST                 /mailboxes                                                       1402      5      6      7      8     10     14     20     27     66     67     67
 DELETE               delete message                                                   4139      5      6      7      8     10     13     17     22     51     65     65
 POST                 post message                                                    26376      5      6      7      8     11     14     19     25     60     88     99
 GET                  read message                                                     4196      5      6      7      8     10     13     18     23     64     74     74
 GET                  read messages by page                                            4244      5      6      7      8     10     13     17     20     53     68     68
 DELETE               remove mailbox                                                    381      5      6      7      8     10     13     16     19     25     25     25
------------------------------------------------------------------------------------------------------------------------------------------------------
```
 
This looks surprisingly slow, to be honest. I would need some time to look into it

## Implementation plan

[x] Design     
[x] Core       
[x] Rest api   
[ ] Monitoring  (via finatra /admin)
