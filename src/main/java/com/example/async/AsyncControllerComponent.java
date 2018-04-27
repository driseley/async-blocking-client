package com.example.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AsyncControllerComponent implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(AsyncControllerComponent.class);

	public AsyncControllerComponent() {
		logger.debug("Created new AsyncControllerComponent");
	}

	@Autowired
	private ExecutorService executorService;

	@Override
	public void run() {
		logger.info("AsyncJob started");

		List<Future<String>> results = new ArrayList<>();
		long submissionStartTime = System.currentTimeMillis();
		for (int i=0; i<5000; i++) {
			Future<String> result = executorService.submit(new JobTask(i));
			results.add(result);
			if ( i%50 == 0 ) {
				logger.info("{} tasks submitted",i);
			}
		}
		long submissionEndTime=System.currentTimeMillis();

		int doneCount=0;
		long workStartTime = System.currentTimeMillis();
		while (doneCount < results.size() ) {
			List<Future<String>> complete = results.stream()
					.filter(r -> r.isDone())
					.collect(Collectors.toList());
			doneCount = complete.size();
			logger.info ("{} done, performing useful work", doneCount);
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				logger.warn("Sleep interrupted");
			}
		}
		long workEndTime=System.currentTimeMillis();

		logger.info("All tasks complete");

		logger.info("Submission took {} ms", (submissionEndTime-submissionStartTime));
		logger.info("Time for useful work {} ms", (workEndTime-workStartTime));
	}
}
