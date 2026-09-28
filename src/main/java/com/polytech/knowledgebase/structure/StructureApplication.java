package com.polytech.knowledgebase.structure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StructureApplication {

	public static void main(String[] args) {
		try (var context = SpringApplication.run(StructureApplication.class, args)) {
			// The console runner ends on exit or EOF; release the pool and JPA resources.
		}
	}

}
