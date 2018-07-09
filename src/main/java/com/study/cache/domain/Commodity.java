package com.study.cache.domain;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jerry Xie
 * @date 7/6/2018 15:15
 */
@AllArgsConstructor
@Slf4j
public class Commodity {

    private String name;

    private String value;

    public String getName() {
        log.info("enter into the get name method in {}.",  this.getClass().getSimpleName());
        return name;
    }

    public void setName(String name) {
        log.info("enter into the set name method in {}.", this.getClass().getSimpleName());
        this.name = name;
    }

    public String getValue() {
        log.info("enter into the get value method in {}.", this.getClass().getSimpleName());
        return value;
    }

    public void setValue(String value) {
        log.info("enter into the set value method in {}.", this.getClass().getSimpleName());
        this.value = value;
    }
}
