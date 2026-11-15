package com.taskmaster;

import com.taskmaster.config.TaskMasterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TaskMasterProperties.class)
public class TaskMasterApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskMasterApplication.class, args);
	}
}
