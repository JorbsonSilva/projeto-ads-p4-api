package com.equipe.projetoadsapi.tutor.security;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class RagEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var environment = context.getEnvironment();
        return Binder.get(environment).bind("orienta.rag.enabled", Boolean.class).orElseGet(() ->
                StringUtils.hasText(environment.getProperty("orienta.rag.internal-secret"))
                && StringUtils.hasText(environment.getProperty("orienta.rag.subject-secret")));
    }
}
