package com.ead.gearup.controller;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GraphQLHealthCheckController {

    @QueryMapping
    public String healthCheck() {
        return "GraphQL is working successfully";
    }
}
