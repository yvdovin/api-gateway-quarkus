package ru.tsc.crm.util.graphql;

import graphql.GraphQLContext;
import graphql.GraphQLError;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLInputType;
import graphql.validation.constraints.AbstractDirectiveConstraint;
import graphql.validation.constraints.Documentation;
import graphql.validation.rules.ValidationEnvironment;

import java.util.Collections;
import java.util.List;

public class DictionaryConstraint extends AbstractDirectiveConstraint {
    public DictionaryConstraint() {
        super("Dictionary");
    }

    @Override
    protected boolean appliesToType(GraphQLInputType inputType) {
        return isStringOrIDOrListOrMap(inputType);
    }

    @Override
    protected List<GraphQLError> runConstraint(ValidationEnvironment validationEnvironment) {
        System.out.println("Start constraint");
        Object validatedValue = validationEnvironment.getValidatedValue();

        if (validatedValue == null) {
            return Collections.emptyList();
        }

        GraphQLInputType argType = validationEnvironment.getValidatedType();

        GraphQLDirective directive = validationEnvironment.getContextObject(GraphQLDirective.class);
        //Вот тут хочется вытащить какой-нибудь контекст и записать туда все поля из директивы,
        // чтобы в будущем их проверить одним разом
        GraphQLContext context = validationEnvironment.getContextObject(GraphQLContext.class);

        if (context != null) {
            System.out.println("????????????");
        }

        return Collections.emptyList();
    }

    @Override
    public Documentation getDocumentation() {
        return null;
    }
}
