package org.springsource.pwebb.spike.cloudfoundry.timeout.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Example MVC controller used to simulate long running AJAX requests.
 * 
 * @author Phillip Webb
 */
@Controller
public class TimeoutExampleController {

	@RequestMapping("/example")
	public void example() {
	}

	@RequestMapping("/ajaxrequest")
	@ResponseBody
	public String ajaxRequest() {
		System.out.println("Ajax");
		try {
			for (int i = 1; i <= 40; i++) {
				Thread.sleep(1000);
				System.out.println("Thinking..." + i);
			}
		} catch (InterruptedException e) {
		}
		return "ajax response";
	}

}
