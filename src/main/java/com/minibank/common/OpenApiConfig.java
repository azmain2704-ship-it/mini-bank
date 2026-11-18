package com.minibank.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The title and description shown at the top of Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI miniBankApi() {
        return new OpenAPI().info(new Info()
                .title("Mini Bank API")
                .version("1.0")
                .description("""
                        The behind-the-scenes part of a bank: accounts, transfers that can't overdraw (even when lots \
                        arrive at once), a full history and fraud flags. Java 21, Spring Boot and PostgreSQL.

                        **Quick try:** `GET /api/accounts` lists the demo accounts. Then `POST /api/transfers` with \
                        two of those ids and an amount, e.g. `{"fromAccountId": 1, "toAccountId": 2, "amount": 5.00}`. Send more than the balance to see \
                        a refused transfer, or £5,000+ to set off a fraud flag.

                        Source code: https://github.com/azmain2704-ship-it/mini-bank"""));
    }
}
