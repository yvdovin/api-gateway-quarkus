package ru.tsc.crm.controller;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.spi.HttpRequest;
import ru.tsc.crm.util.graphql.DictionaryConstraint;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.scalars.ExtendedScalars.*;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

@Path("")
public class GraphQLController {

    public static void main(String[] args) {
        TypeDefinitionRegistry registry = new SchemaParser().parse(schema1);
        //registry.merge(new SchemaParser().parse(schema2));
        System.out.println(registry.scalars());
        System.out.println(registry.getDirectiveDefinition("Dictionary"));

//        String query = "query{\n" +
//                "\tgetById(id: 1){\n" +
//                "\t\tshortName\n" +
//                "\t}\n" +
//                "}";

        String query = """
                mutation{
                	update(
                		req: {
                			shortName: "ad",
                			officialName: "a"
                		}
                	){
                		shortName
                	}
                }
                """;

//                String query = """
//                query{
//                	getById(
//                		req: {
//                			shortName: "ad"
//                		}
//                	){
//                		shortName
//                	}
//                }
            //    """;
        RuntimeWiring.Builder builder = newRuntimeWiring();
        var queries = resolveMethods(registry, "Query");
        var mutations = resolveMethods(registry, "Mutation");
        var validationRules = ValidationRules.newValidationRules()
                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
                .addRule(new DictionaryConstraint())
                .build();
        RuntimeWiring runtimeWiring = builder
                .directiveWiring(new ValidationSchemaWiring(validationRules))
                .scalar(GraphQLLong)
                .scalar(DateTime)
                .scalar(Date)
                .type("Query", builder1 -> resolveBuilder(queries, builder1))
                .type("Mutation", builder1 -> resolveBuilder(mutations, builder1))
                .build();

        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        ExecutionResult execute = graphQL.execute(query);
        System.out.println(resolveMethods(registry, "Query"));
        System.out.println("!!!!!!!!!!" + execute.getErrors());
        System.out.println("&&&&&&&&&&&&&&&" + execute.getData());
    }

    private static TypeRuntimeWiring.Builder resolveBuilder(List<String> methods, TypeRuntimeWiring.Builder builder) {
        var dataFetcherMap = methods.stream()
                .collect(Collectors.toMap(name -> name, name -> (DataFetcher) environment -> {
                    return Map.of("shortName", "Bob");
                }));
        return builder.dataFetchers(dataFetcherMap);
    }

    @POST
    @Path("{var:.+}/graphql/{var2:.+}")
    public Uni<Response> post(@Context HttpRequest request, byte[] body) {
        TypeDefinitionRegistry registry = new SchemaParser().parse(schema1);

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
                .build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(registry, runtimeWiring);

        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        graphQL.execute(new String(body));
        return Uni.createFrom().item(Response.ok("hello").build());
    }

    private static List<String> resolveMethods(TypeDefinitionRegistry typeRegistry, String query) {
        return typeRegistry.getType(query)
                .map(ObjectTypeDefinition.class::cast)
                .map(ObjectTypeDefinition::getChildren)
                .stream()
                .flatMap(Collection::stream)
                .map(FieldDefinition.class::cast)
                .map(FieldDefinition::getName)
                .toList();
    }

    private static String schema1 = """
            schema {
                query: Query
            	mutation: Mutation
            }
                        
            type Query {
                getById(id: Long!): GetCompanyByIdResponse!
            }
                        
            type Mutation {
            	update(req: MutationRequest!) : MutationResponse!
            }
                        
            directive @Dictionary(message : String = "Неверное количество символов") on INPUT_FIELD_DEFINITION
                        
            input MutationRequest {
            	shortName : String
                officialName : String @Dictionary
            }
                        
            type MutationResponse {
            	shortName : String
                officialName : String
            }
                        
            type GetCompanyByIdResponse {
                shortName : String
                officialName : String
            }
                        
            scalar Long
            """;

    private static String schema2 = """
            schema {
                query: Query2
            	mutation: Mutation2
            }
                        
            type Query2 {
                getById2(id: Long!): GetCompanyByIdResponse2!
            }
                        
            type Mutation2 {
            	update2(req: MutationRequest2!) : MutationResponse2!
            }
                        
            directive @Dictionary2(message : String = "Неверное количество символов") on INPUT_FIELD_DEFINITION
                        
            input MutationRequest2 {
            	shortName : String @Dictionary2
                officialName : String
            }
                        
            type MutationResponse2 {
            	shortName : String
                officialName : String
            }
                        
            type GetCompanyByIdResponse2 {
                shortName : String
                officialName : String
            }
                        
            scalar Long2
            """;
}
