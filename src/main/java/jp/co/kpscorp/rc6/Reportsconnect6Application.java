package jp.co.kpscorp.rc6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("jp.co.kpscorp")
//@ComponentScan(basePackages = "jp.co.kpscorp", excludeFilters = @Filter(type = FilterType.REGEX, pattern = "jp.co.kpscorp.scon.*"))
@ComponentScan("jp.co.kpscorp")
@EntityScan("jp.co.kpscorp")
//@EnableAsync
public class Reportsconnect6Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Reportsconnect6Application.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(Reportsconnect6Application.class, args);
	}

}
