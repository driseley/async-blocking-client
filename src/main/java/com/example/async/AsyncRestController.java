package com.example.async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncRestController {
	
	private static final Logger logger = LoggerFactory.getLogger(AsyncRestController.class);
	
	@Autowired
	private AsyncControllerComponent asyncControllerComponent;
	
    @RequestMapping("/job/start")
    public String start() {
    	logger.info("Starting Async Job");
		Thread t = new Thread(asyncControllerComponent);
		t.start();  	   	
        return "Async Job Started";
    }
    
}

