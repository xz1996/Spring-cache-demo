package com.study.cache.service;

import com.study.cache.domain.Commodity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Jerry Xie
 * @date 7/6/2018 15:58
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CommodityServiceTest {

    @Autowired
    private CommodityService commodityService;

    @Autowired
    private RedisCacheManager redisCacheManager;

    private Commodity commodity;

    private Cache redisCache;

    @Before
    public void setUp() {

        this.commodity = new Commodity("book", "20");
        commodityService.setCommodity(commodity);
        redisCache = redisCacheManager.getCache("commodity");
    }

    @Test
    public void getName() {

        System.out.println(commodityService.getName());
        System.out.println("second service: " + commodityService.getName());
    }

    @Test
    public void getValue() {

        System.out.println(commodity.getValue());
    }

    @Test
    public void setName() {

    }

    @Test
    public void setValue() {
    }

    @Test
    public void delete() {
    }

    @Test
    public void testCahe() throws InterruptedException {

        // Cache the getName value, the key is "getName".
        System.out.println("before cache: " + commodityService.getName());

        // Cache the setName value, the key is "new one".
        commodityService.setName("new one");
        System.out.println("after set: " + commodityService.getName());

        // Wait until the expiration of cache.
        Thread.sleep(10_000);
        System.out.println("after set 10 sec: " + commodityService.getName());
    }

    @Test
    public void testCacheManager() {

        redisCache.put("key", "123");
        System.out.println(redisCache.get("key").get());
    }

    @After
    public void tearDown() {

        // Clear all the cache.
        redisCacheManager.getCacheNames().forEach(cacheName -> redisCacheManager.getCache(cacheName).clear());
    }
}