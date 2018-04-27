package com.example.async;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

@Configuration
@Profile("controller")
public class AsyncControllerConfiguration {

	@Value("${hazelcast.listen.address}")
	private String hazelcastListenAddress;

	@Value("${hazelcast.executor.name}")
	private String hazelcastExecutorName;

	@Value("${local.executor.queuecapacity}")
	private int localExecutorQueueCapacity;

	@Value("${local.executor.poolsize}")
	private int localExecutorPoolSize;

	@Bean
	@Profile("client")
	public ClientConfig hazelcastClientConfig() {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.getNetworkConfig().addAddress(hazelcastListenAddress);
		return clientConfig;
	}

	@Bean(destroyMethod = "")
	@Profile("client")
	public ExecutorService distributedExecutorService(HazelcastInstance instance) {
		return instance.getExecutorService(hazelcastExecutorName);
	}

	@Bean
	@Profile("local")
	public TaskExecutor jobLocalTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(localExecutorPoolSize);
		executor.setMaxPoolSize(localExecutorPoolSize);
		executor.setQueueCapacity(localExecutorQueueCapacity);
		return executor;
	}

	@Bean
	@Profile("local")
	public ExecutorService springExecutorService() {
		ExecutorServiceAdapter esa = new ExecutorServiceAdapter(jobLocalTaskExecutor());
		return esa;
	}

	@Bean
	public AsyncControllerComponent asyncControllerComponent() {
		return new AsyncControllerComponent();
	}

}
