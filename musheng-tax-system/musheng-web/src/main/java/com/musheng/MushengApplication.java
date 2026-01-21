package com.musheng;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 慕声税务系统应用程序
 * 亚马逊跨境贸易税务系统
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.musheng.**.mapper")
public class MushengApplication {

    public static void main(String[] args) {
        SpringApplication.run(MushengApplication.class, args);
        System.out.println("============================================");
        System.out.println("  慕声税务系统启动成功!                      ");
        System.out.println("============================================");
    }
}
