package io.agenticai.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.Arrays;

/**
 * One springdoc group per module, so each module's served surface is its own OpenAPI document at
 * {@code /v3/api-docs/<group>}. Read by {@code governance/governance-tools/api-doc-generator}
 * (its discovery matches {@code --module REG} to the group whose bean method / group id / display
 * name contains the module code, and scopes the module's source root to the scanned package).
 * Documentation only: no endpoint, mapping or behaviour changes here (see {@link #multipartBodies()}). Owned by the platform track
 * because it spans every module of the one deployable.
 */
@Configuration
public class OpenApiGroupsConfiguration {

    /**
     * Documents a handler whose body is bound through {@code @RequestPart} as
     * {@code multipart/form-data}. springdoc files such a body under its default consumes media type
     * ({@code application/json}) unless the mapping declares {@code consumes}, and a declared
     * {@code consumes} would refuse a wrong Content-Type in handler mapping — before any handler is
     * known, so outside the module's ProblemAdvice. Documentation only: the request is bound exactly
     * as before; the schema springdoc computed is kept, only its media type key changes.
     */
    static OperationCustomizer multipartBodies() {
        return (operation, handlerMethod) -> {
            RequestBody body = operation.getRequestBody();
            boolean parts = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(RequestPart.class));
            if (parts && body != null && body.getContent() != null) {
                MediaType json = body.getContent().remove(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                if (json != null) {
                    body.getContent().addMediaType(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE, json);
                }
                boolean required = Arrays.stream(handlerMethod.getMethodParameters())
                        .map(parameter -> parameter.getParameterAnnotation(RequestPart.class))
                        .anyMatch(part -> part != null && part.required());
                if (required) {
                    body.setRequired(true);
                }
            }
            return operation;
        };
    }

    /** The {@code info} every group's document carries (title and API version). */
    @Bean
    public OpenAPI aiasOpenApi() {
        return new OpenAPI().info(new Info().title("aias — Request Verification Service").version("v1"));
    }

    @Bean
    public GroupedOpenApi regApi() {
        return GroupedOpenApi.builder().group("reg").displayName("REG").packagesToScan("io.agenticai.reg")
                .addOperationCustomizer(multipartBodies()).build();
    }

    @Bean
    public GroupedOpenApi docApi() {
        return GroupedOpenApi.builder().group("doc").displayName("DOC").packagesToScan("io.agenticai.doc")
                .addOperationCustomizer(multipartBodies()).build();
    }

    @Bean
    public GroupedOpenApi chkApi() {
        return GroupedOpenApi.builder().group("chk").displayName("CHK").packagesToScan("io.agenticai.chk")
                .addOperationCustomizer(multipartBodies()).build();
    }

    @Bean
    public GroupedOpenApi rptApi() {
        return GroupedOpenApi.builder().group("rpt").displayName("RPT").packagesToScan("io.agenticai.rpt")
                .addOperationCustomizer(multipartBodies()).build();
    }

    @Bean
    public GroupedOpenApi intApi() {
        return GroupedOpenApi.builder().group("int").displayName("INT").packagesToScan("io.agenticai.integration")
                .addOperationCustomizer(multipartBodies()).build();
    }
}
