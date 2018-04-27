package com.example.async;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.HazelcastInstance;

@Configuration
@Profile({"member"})
public class AsyncWorkerConfiguration {

	@Value("${hazelcast.listen.address}")
	private String hazelcastListenAddress;

	@Value("${hazelcast.executor.name}")
	private String hazelcastExecutorName;

	@Value("${hazelcast.executor.queuecapacity}")
	private int hazelcastExecutorQueueCapacity;

	@Value("${hazelcast.executor.poolsize}")
	private int hazelcastExecutorPoolSize;

	@Bean
	public Config hazelcastConfig() {
		Config config = new Config();

		// Disable multicast
		JoinConfig joinConfig = config.getNetworkConfig().getJoin();
		joinConfig.getMulticastConfig().setEnabled(false);

		// Enable TCP/IP
		joinConfig.getTcpIpConfig().setEnabled(true).addMember(hazelcastListenAddress);

		// Configure the executor pool
		config.getExecutorConfig(hazelcastExecutorName)
			.setPoolSize(hazelcastExecutorPoolSize)
			.setQueueCapacity(hazelcastExecutorQueueCapacity)
			.setStatisticsEnabled(true);

		return config;
	}

	@Bean(destroyMethod = "")
	public ExecutorService distributedExecutorService(HazelcastInstance instance) {
		return instance.getExecutorService(hazelcastExecutorName);
	}

}
