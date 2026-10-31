package br.com.estudos.helpdesk.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL real em container para os testes de integração. Como bean, o container acompanha o ciclo
 * de vida do contexto do Spring (e é reaproveitado enquanto o contexto estiver em cache).
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlContainerConfig {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}
