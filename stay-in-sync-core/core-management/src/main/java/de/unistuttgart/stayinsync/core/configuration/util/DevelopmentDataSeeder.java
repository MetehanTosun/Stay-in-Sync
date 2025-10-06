package de.unistuttgart.stayinsync.core.configuration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.*;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.ActionDefinitionDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.CreateArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.UpdateTransformationRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.service.*;
import de.unistuttgart.stayinsync.transport.ScriptStatus;
import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationPatternType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class DevelopmentDataSeeder {

    @Inject
    AfterSeedRunner afterSeedRunner;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    TargetSystemService targetSystemService;

    @Inject
    TransformationService transformationService;

    @Inject
    TransformationScriptService transformationScriptService;

    @Inject
    SourceSystemApiRequestConfigurationService sourceSystemApiRequestConfigurationService;

    @Inject
    TargetSystemApiRequestConfigurationService targetSystemApiRequestConfigurationService;

    @Transactional
    void seedDatabase() {
        if (!"dev".equals(profile) && !"test".equals(profile)) {
            return;
        }

        Transformation.deleteAll();
        SourceSystemApiRequestConfiguration.deleteAll();
        TargetSystemApiRequestConfiguration.deleteAll();
        SourceSystem.deleteAll();
        TargetSystem.deleteAll();
        Log.info("====== CLEARED OLD DATA ======");

        Log.info("====== STARTING DATABASE SEEDING FOR DEVELOPMENT ======");

        CreateSourceSystemDTO sourceDto = createSourceSystemDTO();
        SourceSystem sourceSystem = sourceSystemService.createSourceSystem(sourceDto);

        SyncSystemEndpoint sourceEndpoint = sourceSystem.syncSystemEndpoints.stream()
                .filter(endpoint ->
                        endpoint != null &&
                                endpoint.httpRequestType != null &&
                                endpoint.endpointPath != null &&
                                "GET".equals(endpoint.httpRequestType.trim()) &&
                                "/products".equals(endpoint.endpointPath.trim())
                )
                .findFirst()
                .orElseThrow(() -> {
                    // Erstelle eine aussagekräftige Fehlermeldung, die uns alle gefundenen Pfade zeigt.
                    String availableEndpoints = sourceSystem.syncSystemEndpoints.stream()
                            .map(e -> e.httpRequestType + " " + e.endpointPath)
                            .collect(Collectors.joining(", "));

                    return new IllegalStateException(
                            "GET /products endpoint not found in source system. Endpoints found: 9. Available endpoints are: [" + availableEndpoints + "]"
                    );
                });

        CreateSourceArcDTO sourceArcDTO = createSourceArcDTO(sourceSystem, sourceEndpoint);
        SourceSystemApiRequestConfiguration sourceArc = sourceSystemApiRequestConfigurationService.create(sourceArcDTO, sourceEndpoint.id);
        Log.info("-> Created SourceSystem 'Dummy_JSON' and Source ARC 'products'");

        TargetSystemDTO targetDto = new TargetSystemDTO(
                null, sourceDto.name(), sourceDto.apiUrl(), sourceDto.description(),
                sourceDto.apiType(), sourceDto.openApiSpec(), null);
        TargetSystemDTO targetSystemDTO = targetSystemService.createTargetSystem(targetDto);
        TargetSystem targetSystem = TargetSystem.findById(targetSystemDTO.id());
        Log.info("-> Created TargetSystem 'Dummy_JSON'");

        TargetSystemApiRequestConfiguration targetArc = createProductUpsertTargetArc(targetSystem);
        Log.info("-> Created Target ARC 'synchronizeProducts'");

        TransformationShellDTO shellDTO = createTransformationShellDTO();
        Transformation transformation = transformationService.createTransformation(shellDTO);
        Log.info("-> Created Transformation 'TestTransformation'");

        UpdateTransformationRequestConfigurationDTO updateTargetArcsDto = new UpdateTransformationRequestConfigurationDTO(Set.of(targetArc.id),Collections.emptySet());
        transformationService.updateTargetArcs(transformation.id, updateTargetArcsDto);
        Log.info("-> Linked Target ARC to Transformation");

        TransformationScriptDTO scriptDTO = createFinalTransformationScriptDTO(transformation.id, sourceArc.alias, targetArc.id);
        transformationScriptService.saveOrUpdateForTransformation(transformation.id, scriptDTO);
        Log.info("-> Created and linked final Transformation Script");

        //afterSeedRunner.runAfterSeed();

        Log.info("====== DATABASE SEEDING FINISHED ======");
    }

    private TargetSystemApiRequestConfiguration createProductUpsertTargetArc(TargetSystem targetSystem) {
        SyncSystemEndpoint checkEndpoint = targetSystem.syncSystemEndpoints.stream()
                .filter(e -> "GET".equals(e.httpRequestType) && "/products/search".equals(e.endpointPath))
                .findFirst().orElseThrow(() -> new IllegalStateException("Endpoint GET /products/search not found"));

        SyncSystemEndpoint createEndpoint = targetSystem.syncSystemEndpoints.stream()
                .filter(e -> "POST".equals(e.httpRequestType) && "/products/add".equals(e.endpointPath))
                .findFirst().orElseThrow(() -> new IllegalStateException("Endpoint POST /products/add not found"));

        SyncSystemEndpoint updateEndpoint = targetSystem.syncSystemEndpoints.stream()
                .filter(e -> "PUT".equals(e.httpRequestType) && "/products/{product_id}".equals(e.endpointPath))
                .findFirst().orElseThrow(() -> new IllegalStateException("Endpoint PUT /products/{product_id} not found"));

        List<ActionDefinitionDTO> actions = List.of(
                new ActionDefinitionDTO(checkEndpoint.id, TargetApiRequestConfigurationActionRole.CHECK, 1),
                new ActionDefinitionDTO(createEndpoint.id, TargetApiRequestConfigurationActionRole.CREATE, 2),
                new ActionDefinitionDTO(updateEndpoint.id, TargetApiRequestConfigurationActionRole.UPDATE, 3)
        );

        CreateArcDTO targetArcDto = new CreateArcDTO(
                "synchronizeProducts",
                targetSystem.id,
                TargetApiRequestConfigurationPatternType.OBJECT_UPSERT,
                actions
        );

        return targetSystemApiRequestConfigurationService.create(targetArcDto);
    }

    private CreateSourceSystemDTO createSourceSystemDTO() {
        CreateSourceSystemDTO dto = new CreateSourceSystemDTO(
                null,
                "Dummy_JSON",
                "https://dummyjson.com",
                "Some DummyData here",
                "REST",
                ApiAuthType.BASIC,
                new BasicAuthDTO("admin", "secret123"),
                "{ \"openapi\": \"3.0.0\", \"info\": { \"title\": \"DummyJSON API\", \"version\": \"0.0.5\", \"description\": \"DummyJSON API\" }, \"externalDocs\": { \"description\": \"swagger.json\", \"url\": \"/api-docs/swagger.json\" }, \"servers\": [], \"security\": [], \"tags\": [ { \"name\": \"Product\", \"description\": \"The product API\" } ], \"paths\": { \"/products\": { \"get\": { \"summary\": \"get all products\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"query\", \"name\": \"limit\", \"schema\": { \"type\": \"number\" }, \"required\": false }, { \"in\": \"query\", \"name\": \"skip\", \"schema\": { \"type\": \"number\" }, \"required\": false }, { \"in\": \"query\", \"name\": \"select\", \"schema\": { \"type\": \"string\" }, \"required\": false } ], \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Products\" } } } }, \"500\": { \"description\": \"error\" } } } }, \"/products/search\": { \"get\": { \"summary\": \"search products\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"query\", \"name\": \"q\", \"description\": \"searchQuery\", \"schema\": { \"type\": \"string\" }, \"required\": false }, { \"in\": \"query\", \"name\": \"limit\", \"schema\": { \"type\": \"number\" }, \"required\": false }, { \"in\": \"query\", \"name\": \"skip\", \"schema\": { \"type\": \"number\" }, \"required\": false }, { \"in\": \"query\", \"name\": \"select\", \"schema\": { \"type\": \"string\" }, \"required\": false } ], \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Products\" } } } }, \"500\": { \"description\": \"error\" } } } }, \"/products/categories\": { \"get\": { \"summary\": \"get all products categories\", \"tags\": [\"Product\"], \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } } } } } } } }, \"/products/category/{category_name}\": { \"get\": { \"summary\": \"get products of category\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"path\", \"name\": \"category_name\", \"description\": \"categorName\", \"schema\": { \"type\": \"string\" }, \"required\": true }, { \"in\": \"query\", \"name\": \"select\", \"schema\": { \"type\": \"string\" }, \"required\": false } ], \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Products\" } } } }, \"500\": { \"description\": \"error\" } } } }, \"/products/add\": { \"post\": { \"summary\": \"create a new product\", \"tags\": [\"Product\"], \"requestBody\": { \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"500\": { \"description\": \"error\" } } } }, \"/products/{product_id}\": { \"get\": { \"summary\": \"get product by id\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"path\", \"name\": \"product_id\", \"schema\": { \"type\": \"integer\" }, \"required\": true }, { \"in\": \"query\", \"name\": \"select\", \"schema\": { \"type\": \"string\" }, \"required\": false } ], \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"500\": { \"description\": \"error\" } } }, \"put\": { \"summary\": \"update a product\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"path\", \"name\": \"product_id\", \"schema\": { \"type\": \"integer\" }, \"required\": true } ], \"requestBody\": { \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"500\": { \"description\": \"error\" } } }, \"patch\": { \"summary\": \"update a product\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"path\", \"name\": \"product_id\", \"schema\": { \"type\": \"integer\" }, \"required\": true } ], \"requestBody\": { \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/Product\" } } } }, \"500\": { \"description\": \"error\" } } }, \"delete\": { \"summary\": \"delete a product\", \"tags\": [\"Product\"], \"parameters\": [ { \"in\": \"path\", \"name\": \"product_id\", \"schema\": { \"type\": \"integer\" }, \"required\": true } ], \"responses\": { \"200\": { \"description\": \"success\", \"content\": { \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/ProductDelete\" } } } }, \"500\": { \"description\": \"error\" } } } } }, \"components\": { \"schemas\": { \"Products\": { \"type\": \"object\", \"properties\": { \"products\": { \"type\": \"array\", \"items\": { \"$ref\": \"#/components/schemas/Product\" } }, \"total\": { \"type\": \"number\" }, \"skip\": { \"type\": \"number\" }, \"limit\": { \"type\": \"number\" } } }, \"Product\": { \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"number\" }, \"title\": { \"type\": \"string\" }, \"description\": { \"type\": \"string\" }, \"price\": { \"type\": \"number\" }, \"discountPercentage\": { \"type\": \"number\" }, \"rating\": { \"type\": \"number\" }, \"stock\": { \"type\": \"number\" }, \"brand\": { \"type\": \"string\" }, \"category\": { \"type\": \"string\" }, \"thumbnail\": { \"type\": \"string\" }, \"images\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } } } }, \"ProductDelete\": { \"allOf\": [ { \"$ref\": \"#/components/schemas/Product\" }, { \"type\": \"object\", \"properties\": { \"isDeleted\": { \"type\": \"boolean\" }, \"deletedOn\": { \"type\": \"string\" } } } ] } } }, \"webhooks\": {} }"
        );
        return dto;
    }

    private CreateSourceArcDTO createSourceArcDTO(SourceSystem sourceSystem, SyncSystemEndpoint sourceSystemEndpoint) {
        Map<String, Object> exampleProduct = Map.of(
                "id", 1,
                "title", "iPhone 9",
                "description", "An apple mobile which is nothing like apple",
                "price", 549,
                "sku", "HV-1337"
        );

        Map<String, Object> examplePayload = Map.of(
                "products", List.of(exampleProduct),
                "total", 100,
                "skip", 0,
                "limit", 1
        );

        String responsePayloadAsString;
        try {
            responsePayloadAsString = objectMapper.writeValueAsString(examplePayload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize example payload for ARC seeder", e);
        }


        return new CreateSourceArcDTO(
                "syncProductsArc",
                sourceSystem.id,
                sourceSystemEndpoint.id,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                responsePayloadAsString,
                20000, // 20 seconds
                false
        );
    }

    private TransformationShellDTO createTransformationShellDTO() {
        TransformationShellDTO dto = new TransformationShellDTO(
                "TestTransformation",
                "This Transformation is auto-generated and is supposed to assist in development."
        );
        return dto;
    }

    private TransformationScriptDTO createFinalTransformationScriptDTO(Long transformationId, String sourceArcAlias, Long targetArcId) {
        String scriptCode = """
                /**
                 * Transforms products from the source into upsert directives for the target.
                 */
                function transform() {
                    stayinsync.log('Transformation started: Upserting products...', 'INFO');
                
                    const products = source.Dummy_JSON.products.products;
                    const productsFromSource = products.slice(1,2);
                
                    if (!productsFromSource || productsFromSource.length === 0) {
                        stayinsync.log('No products found in source data. Finishing.', 'WARN');
                        return { synchronizeProducts: [] };
                    }
                
                    const directives = productsFromSource.map(product => {
                        return targets.synchronizeProducts.defineUpsert()
                            // CHECK: Find product with its title. Assumption: sku is unique
                            .usingCheck(config => {
                                config.withQueryParamQ(product.title); // DummyJSON 'q' is search param
                            })
                            // CREATE: If not found, create new Product
                            .usingCreate(config => {
                                config.withPayload({
                                    title: product.title,
                                    description: "New: " + product.description,
                                    price: product.price
                                });
                            })
                            // UPDATE: If found, update price and description
                            .usingUpdate(config => {
                                config.withPathParamProductId(checkResponse => {
                                    // Assumption: /products/search-answer contains a list and we take the first occurence
                                    return checkResponse.products[0].id;
                                }).withPayload({
                                    price: product.price,
                                    description: "Updated: " + product.description
                                });
                            })
                            .build();
                    });
                
                    stayinsync.log(`Transformation finished. Created ${directives.length} directives.`, 'INFO');
                
                    return {
                        synchronizeProducts: directives
                    };
                }
                """;

        return new TransformationScriptDTO(
                transformationId,
                "Product Upsert Script",
                scriptCode,
                scriptCode,
                Set.of(sourceArcAlias),
                ScriptStatus.VALIDATED,
                Set.of(targetArcId),
                Set.of()
        );
    }
}
