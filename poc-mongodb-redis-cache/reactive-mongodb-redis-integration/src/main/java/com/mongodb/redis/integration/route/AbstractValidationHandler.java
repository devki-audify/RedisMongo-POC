/* Licensed under Apache-2.0 2021-2022 */
package com.mongodb.redis.integration.route;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public abstract class AbstractValidationHandler<T, U extends Validator> {

    private final Class<T> validationClass;

    private final U validator;

    protected AbstractValidationHandler(Class<T> clazz, U validator) {
        this.validationClass = clazz;
        this.validator = validator;
    }

    protected abstract Mono<ServerResponse> processBody(
            T validBody, final ServerRequest originalRequest);

    public final Mono<ServerResponse> handleRequest(final ServerRequest request) {
        return request.bodyToMono(this.validationClass)
                .flatMap(
                        body -> {
                            Errors errors =
                                    new BeanPropertyBindingResult(
                                            body, this.validationClass.getName());
                            this.validator.validate(body, errors);

                            if (errors.getAllErrors().isEmpty()) {
                                return processBody(body, request);
                            } else {
                                return onValidationErrors(errors, body, request);
                            }
                        });
    }

    protected Mono<ServerResponse> onValidationErrors(
            Errors errors, T invalidBody, final ServerRequest request) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors.getAllErrors().toString());
    }
}
