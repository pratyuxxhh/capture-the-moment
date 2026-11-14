package com.example.capture_the_moment.controllers.dev;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/client")
public class WebClientController {

	// THIS CLASS IS JUST FOR TESTING PURPOSE


	@GetMapping
	public String client(@RequestParam String q) {

		if (q.isBlank()) {
			return "Query cannot be blank";
		}
		String targetUri = UriComponentsBuilder
				.fromUriString("http://localhost:8000/query")
				.queryParam("query", q)
				.build()
				.encode()
				.toUriString();
		targetUri = targetUri.replace("%20", " ");
		System.out.println("TARGET URI: " + targetUri);

		try {
			RestClient restClient = RestClient.create();

			return restClient.get()
					.uri(targetUri)
					.retrieve()
					.body(String.class);

		} catch (RuntimeException exception) {
			return "Request failed: " + exception.getMessage();
		}
	}

}
