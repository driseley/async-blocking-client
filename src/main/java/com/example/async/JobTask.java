package com.example.async;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobTask implements Callable<String>, Serializable {
	
	private static final long serialVersionUID = 8945413768030936835L;

	private static final Logger logger = LoggerFactory.getLogger(JobTask.class);
	
	private int requestId;
	
	public JobTask(int requestId) {
		super();
		this.requestId = requestId;
	}

	@Override
	public String call() throws Exception {	
		logger.debug("Invoking task for {}", requestId);
		TimeUnit.MILLISECONDS.sleep(250);
		logger.debug("Task complete for {}", requestId);
		return requestId + " complete";
	}
}
