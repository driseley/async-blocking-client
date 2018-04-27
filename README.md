# Hazelcast distributed executor blocking issue.

See https://groups.google.com/forum/#!topic/hazelcast/3WHwnSOPUpE

We have noticed that if we attempt to submit a large number of Async tasks to a Hazelcast Distributed Executor:

http://docs.hazelcast.org/docs/latest-development/manual/html/Distributed_Computing/Executor_Service/Executing-Code_in_the_Cluster.html

either using the java client or as a full Java member of the cluster, the submissions block.

Here is some example code:

```java
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
```

The JobTask in this case is:

```java
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
```

Using a local task executor (no Hazelcast at all) I get:

```
2018-04-26 17:09:34.885  INFO 14420 --- [       Thread-5] c.e.async.AsyncControllerComponent       : Submission took 14 ms
2018-04-26 17:09:34.886  INFO 14420 --- [       Thread-5] c.e.async.AsyncControllerComponent       : Time for useful work 64043 ms
```

Using a full cluster member I get:

```
2018-04-26 17:12:32.566  INFO 16916 --- [       Thread-5] c.e.async.AsyncControllerComponent       : Submission took 70463 ms
2018-04-26 17:12:32.566  INFO 16916 --- [       Thread-5] c.e.async.AsyncControllerComponent       : Time for useful work 2009 ms
```

Using a Hazelcast client to a remote cluster I get:

```
2018-04-26 17:15:40.649  INFO 7336 --- [       Thread-5] c.e.async.AsyncControllerComponent       : Submission took 68252 ms
2018-04-26 17:15:40.649  INFO 7336 --- [       Thread-5] c.e.async.AsyncControllerComponent       : Time for useful work 2011 ms
```

In each case, I have configured the executor as follows, to have a pool size of 25 threads and queue capacity of 10,000 (larger than the 5,000 submitted tasks) :

```java
		// Configure the executor pool
		config.getExecutorConfig(hazelcastExecutorName)
			.setPoolSize(hazelcastExecutorPoolSize)
			.setQueueCapacity(hazelcastExecutorQueueCapacity)
			.setStatisticsEnabled(true);
```

Looking at the cluster member case, a thread dump during task submission shows:

```
"Thread-5" #79 daemon prio=5 os_prio=0 tid=0x0000000030dda800 nid=0x4f78 waiting on condition [0x0000000037c8f000]
   java.lang.Thread.State: WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        at java.util.concurrent.locks.LockSupport.park(LockSupport.java:304)
        at com.hazelcast.spi.impl.AbstractInvocationFuture.get(AbstractInvocationFuture.java:153)
        at com.hazelcast.executor.impl.ExecutorServiceProxy.submitToPartitionOwner(ExecutorServiceProxy.java:245)
        at com.hazelcast.executor.impl.ExecutorServiceProxy.submit(ExecutorServiceProxy.java:227)
        at com.example.async.AsyncControllerComponent.run(AsyncControllerComponent.java:34)
        at java.lang.Thread.run(Thread.java:748)
```

Looking at https://github.com/hazelcast/hazelcast/blob/master/hazelcast/src/main/java/com/hazelcast/executor/impl/ExecutorServiceProxy.java#L273 code there is a method:

```java
    /**
     * This is a hack to prevent overloading the system with unprocessed tasks. Once backpressure is added, this can
     * be removed.
     */
    private boolean checkSync() {
        boolean sync = false;
        long last = lastSubmitTime;
        long now = Clock.currentTimeMillis();
        if (last + SYNC_DELAY_MS < now) {
            CONSECUTIVE_SUBMITS.set(this, 0);
        } else if (CONSECUTIVE_SUBMITS.incrementAndGet(this) % SYNC_FREQUENCY == 0) {
            sync = true;
        }
        lastSubmitTime = now;
        return sync;
    }
```

which makes every 100th (SYNC_FREQUENCY) task submission synchronous, causing the blocking behavior.  Similar code exists in the ClientExecutorServiceProxy.java ( MAX_CONSECUTIVE_SUBMITS ).

Is this behaviour by design? If so, could it be made configurable so that if you have large enough queue the limit could be raised or disabled?

Ideally we wanted to have code where we could transparently switch between local and distributed execution.

I have a fully executable test case if it would help - but I think it's clear in the code where and how this is happening, my question is more about whether this behaviour is correct and intentional.

Hope this all makes sense, many thanks in advance

Dave