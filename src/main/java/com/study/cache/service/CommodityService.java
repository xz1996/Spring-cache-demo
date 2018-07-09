package com.study.cache.service;

import com.study.cache.domain.Commodity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author Jerry Xie
 * @date 7/6/2018 15:36
 */
@Slf4j
@Service
public class CommodityService {

    @Getter
    @Setter
    private Commodity commodity;

    @Cacheable(cacheNames = "commodity", key = "#root.methodName")
    public String getName() {
        log.info("enter into the get name method in {}.", this.getClass().getSimpleName());
        return commodity.getName();
    }

    public String getValue() {
        return commodity.getValue();
    }

    @CachePut(cacheNames = "commodity", key = "#root.args[0]")
    public String setName(String name) {

        log.info("enter into the set name method in {}.", this.getClass().getSimpleName() );
        commodity.setName(name);
        return name;
    }

    public void setValue(String value) {

    }

    @CacheEvict(cacheNames = "commodity", key = "#root.args[0]")
    public void delete(String name) {

    }
}
