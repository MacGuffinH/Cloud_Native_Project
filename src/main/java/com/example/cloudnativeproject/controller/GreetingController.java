package com.example.cloudnativeproject.controller;

import com.example.cloudnativeproject.controller.limit.RequestLimit;
import com.example.cloudnativeproject.domain.Greeting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问候控制器
 * 提供REST API接口，实现限流控制
 */
@RestController
public class GreetingController {

    /**
     * Hello接口
     * 返回固定的JSON数据：{"msg": "hello"}
     * 限流策略：每秒最多100次请求
     *
     * @return Greeting对象，包含msg字段
     */
    @GetMapping("/hello")
    @RequestLimit(count=100, time=1000) // 每秒最多100次请求
    public Greeting hello() {
        return new Greeting("hello");
    }
}
