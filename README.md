# Content

- [Introduction](#introduction)
- [Getting started](#getting-started)
  - [the ```build.gradle``` file](#the-buildgradle-file)
  - [the ```application.yml``` file](#the-applicationyml-file)
  - [Cache annotation](#cache-annotation)
  - [```@Cacheable``` annotation](#cacheable-annotation)
    - [Default Key Generation](#default-key-generation)
    - [Custom Key Generation Declaration](#custom-key-generation-declaration)
    - [Available caching SpEL evaluation context](#available-caching-spel-evaluation-context)
  - [```@CachePut``` annotation](#cacheput-annotation)
  - [```@CacheEvict``` annotation](#cacheevict-annotation)
- [Notes](#notes)
- [More info](#more-info)

---

# Introduction

The Spring Framework provides support for transparently adding caching to an application. At its core, the abstraction applies caching to methods, thus reducing the number of executions based on the information available in the cache. The caching logic is applied transparently, without any interference to the invoker. Spring Boot auto-configures the cache infrastructure as long as caching support is enabled via the ```@EnableCaching``` annotation.
This simple example will introduce you how to start cache with redis in spring boot.

requirements:

- JDK1.8

- Spring boot version 2.0.3.RELEASE

- Redis database

# Getting started

## the ```build.gradle``` file

```Gradle

buildscript {
ext {
    springBootVersion = '2.0.3.RELEASE'
}
repositories {
    mavenCentral()
}
dependencies {
    classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
}
}

apply plugin: 'java'
apply plugin: 'eclipse'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'

group = 'com.study'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
    mavenCentral()
}


dependencies {
    compile('org.springframework.boot:spring-boot-starter-cache')
    compile('org.springframework.boot:spring-boot-starter-data-redis')
    compile('org.springframework.boot:spring-boot-starter-web')
    compileOnly('org.projectlombok:lombok')
    testCompile('org.springframework.boot:spring-boot-starter-test')
}

```

## the ```application.yml``` file

```Yml

spring:
  cache:
    type: redis
    cache-names: commodity
    redis:
      # the expiration time of redis cache, "H" means hour, "M" means minute, "S" means second,
      time-to-live: PT10S
  redis:
    host: localhost
    port: 6379

```

We choose the "redis" as the value of spring.cache.type, if you don't want to use any other third lib, you can set it as "simple", then it will use the *ConcurrentHashMap* as a backing Cache store.

After the configuration of above, we should enable the processing of the caching annotations as below:

```Java

@SpringBootApplication
@EnableCaching
public class CacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }
}

```

*You can add the ```@EnableCaching``` annotation to any Bean, but I suggest to add it on the ```@Configuration``` Bean if you have.*

## Cache annotation

For caching declaration, the abstraction provides a set of Java annotations:

* ```@Cacheable``` triggers cache population.

* ```@CacheEvict``` triggers cache eviction.

* ```@CachePut``` updates the cache without interfering with the method execution.

* ```@Caching``` regroups multiple cache operations to be applied on a method.

* ```@CacheConfig``` shares some common cache-related settings at class-level.

### ```@Cacheable``` annotation

As the name implies, @Cacheable is used to demarcate methods that are cacheable - that is, methods for whom the result is stored into the cache so on subsequent invocations (with the same arguments), the value in the cache is returned without having to actually execute the method. In its simplest form, the annotation declaration requires the name of the cache associated with the annotated method:

```Java

@Cacheable(cacheNames = "commodity", key = "#root.methodName")
    public String getName() {
        log.info("enter into the get name method in {}.", this.getClass().getSimpleName());
        return commodity.getName();
    }

```

In the snippet above, the returned reuslt of the method will be cached after the execution of the funtion, the cache name is *"commodity"*, and the key is the method name -- *"getName"*. There is necessary to know the key generation rule.

#### Default Key Generation

Since caches are essentially key-value stores, each invocation of a cached method needs to be translated into a suitable key for cache access. Out of the box, the caching abstraction uses a simple KeyGenerator based on the following algorithm:

- If no params are given, return SimpleKey.EMPTY.

- If only one param is given, return that instance.

- If more the one param is given, return a SimpleKey containing all parameters.

This approach works well for most use-cases; As long as parameters have natural keys and implement valid hashCode() and equals() methods. If that is not the case then the strategy needs to be changed.

To provide a different default key generator, one needs to implement the org.springframework.cache.interceptor.KeyGenerator interface.

#### Custom Key Generation Declaration

Since caching is generic, it is quite likely the target methods have various signatures that cannot be simply mapped on top of the cache structure. This tends to become obvious when the target method has multiple arguments out of which only some are suitable for caching (while the rest are used only by the method logic). For example:

```Java
@Cacheable("books")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

```

At first glance, while the two boolean arguments influence the way the book is found, they are no use for the cache. Further more what if only one of the two is important while the other is not?

For such cases, the ```@Cacheable``` annotation allows the user to specify how the key is generated through its key attribute. The developer can use SpEL to pick the arguments of interest (or their nested properties), perform operations or even invoke arbitrary methods without having to write any code or implement any interface. This is the recommended approach over the default generator since methods tend to be quite different in signatures as the code base grows; while the default strategy might work for some methods, it rarely does for all methods.

Below are some examples of various SpEL declarations - if you are not familiar with it, do yourself a favor and read [Spring Expression Language](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html):

```Java
@Cacheable(cacheNames="books", key="#isbn")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

@Cacheable(cacheNames="books", key="#isbn.rawNumber")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

@Cacheable(cacheNames="books", key="T(someType).hash(#isbn)")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)
```

The snippets above show how easy it is to select a certain argument, one of its properties or even an arbitrary (static) method.

If the algorithm responsible to generate the key is too specific or if it needs to be shared, you may define a custom keyGenerator on the operation. To do this, specify the name of the KeyGenerator bean implementation to use:

```Java
@Cacheable(cacheNames="books", keyGenerator="myKeyGenerator")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)
```

*```The key and keyGenerator parameters are mutually exclusive and an operation specifying both will result in an exception.```*

#### Available caching SpEL evaluation context

Each SpEL expression evaluates again a dedicated context. In addition to the build in parameters, the framework provides dedicated caching related metadata such as the argument names. The next table lists the items made available to the context so one can use them for key and conditional computations:

| Name | Location | Description | Example |
| - | :-: | -: | -: |
|methodName|root object|The name of the method being invoked|#root.methodName|
|target|root objec|The target object being invoked|#root.target|
|args|root object|The arguments (as array) used for invoking the target|#root.args[0]|
|caches|root object|Collection of caches against which the current method is executed|#root.caches[0].name|
|argument name|evaluation context|Name of any of the method arguments. If for some reason the names are not available (e.g. no debug information), the argument names are also available under the #a<#index> where #index stands for the argument index (starting from 0).|#a0 (one can also use #p0 or #p<#index> notation as an alias).|
|result|evaluation context|The result of the method call (the value to be cached). Only available in *unless* expressions, *cache put* expressions (to compute the key), or *cache evict* expressions (when *beforeInvocation* is *false*). For supported wrappers such as Optional, #result refers to the actual object, not the wrapper.|#result|

### ```@CachePut``` annotation

For cases where the cache needs to be updated without interfering with the method execution, one can use the @CachePut annotation. That is, the method will always be executed and its result placed into the cache (according to the @CachePut options). It supports the same options as @Cacheable and should be used for cache population rather than method flow optimization:

```Java
@CachePut(cacheNames = "book", key = "#isbn")
public Book updateBook(ISBN isbn, BookDescriptor descriptor)
```

*```Note that using @CachePut and @Cacheable annotations on the same method is generally strongly discouraged because they have different behaviors. While the latter causes the method execution to be skipped by using the cache, the former forces the execution in order to execute a cache update. This leads to unexpected behavior and with the exception of specific corner-cases (such as annotations having conditions that exclude them from each other), such declaration should be avoided. Note also that such condition should not rely on the result object (i.e. the #result variable) as these are validated upfront to confirm the exclusion.```*

### ```@CacheEvict``` annotation

This process is useful for removing stale or unused data from the cache. ```@CacheEvict``` requires specifying one (or multiple) caches that are affected by the action, allows a custom cache and key resolution or a condition to be specified but in addition, features an extra parameter *allEntries* which indicates whether a cache-wide eviction needs to be performed rather then just an entry one (based on the key):

```Java
@CacheEvict(cacheNames = "books", allEntries = true)
public void loadBooks(InputStream batch)
```

It is important to note that void methods can be used with ```@CacheEvict``` - as the methods act as triggers, the return values are ignored (as they don’t interact with the cache) - this is not the case with ```@Cacheable``` which adds/updates data into the cache and thus requires a result.

# Notes

1. The ```@EnableCaching``` has two mode: *PROXY* and *ASPECTJ*.

    The default advice mode for processing caching annotations is "proxy" which allows for interception of calls through the proxy only; local calls within the same class cannot get intercepted that way. For a more advanced mode of interception, consider switching to "aspectj" mode in combination with compile-time or load-time weaving.

2. Method visibility and cache annotations.

    When using proxies, you should apply the cache annotations only to methods with public visibility. If you do annotate protected, private or package-visible methods with these annotations, no error is raised, but the annotated method does not exhibit the configured caching settings. Consider the use of AspectJ (see below) if you need to annotate non-public methods as it changes the bytecode itself.

    In proxy mode (which is the default), only external method calls coming in through the proxy are intercepted. This means that self-invocation, in effect, a method within the target object calling another method of the target object, will not lead to an actual caching at runtime even if the invoked method is marked with @Cacheable - considering using the aspectj mode in this case. There is an example:
    ```Java
    public String get() {
        return getName();
    }

    @Cacheable(cacheNames = "commodity", key = "#root.methodName")
    public String getName(){...}
    ```
    The cache will not take effect if user calls the *get()* method even if this method calls the *getName()* method  annotated by ```@Cacheable```.

3. The expiration time of cache

    Directly through your cache provider. The solution you are using might support various data policies and different topologies which other solutions do not (take for example the JDK ConcurrentHashMap) - exposing that in the cache abstraction would be useless simply because there would no backing support. Such functionality should be controlled directly through the backing cache, when configuring it or through its native API. So if you use the spring simple cache (which means use *ConcurrentHashMap*), the TTL of cache may be forever.

# More info

If you want to know about the spring cache, click [here](https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache)
