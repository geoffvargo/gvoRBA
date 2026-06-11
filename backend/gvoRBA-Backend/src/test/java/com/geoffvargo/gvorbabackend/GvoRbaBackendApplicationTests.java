package com.geoffvargo.gvorbabackend;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.*;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=" +
			"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
			"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class GvoRbaBackendApplicationTests {
	
	@Test
	void contextLoads() {
	}
	
}
