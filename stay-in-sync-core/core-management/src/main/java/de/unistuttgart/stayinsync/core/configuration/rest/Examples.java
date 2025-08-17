package de.unistuttgart.stayinsync.core.configuration.rest;

final class Examples {

    public static final String VALID_ENDPOINT_PARAM_POST = """
            {
              "paramName": "customerId",
              "queryParamType": "PATH",
              "values": ["123", "456", "789"]
            }
            """;
    public static final String VALID_API_HEADER_POST = """
            {
              "name": "getUserA",
              "used": "false",
              "pollingIntervallInMs": "3"
            }
            """;
    public static final String VALID_API_HEADER_VALUE = """
            """;
    public static final String VALID_ENDPOINT_PARAM_VALUE_POST = """
            """;
    public static final String VALID_EXAMPLE_REQUEST_CONFIGURATION_CREATE = """
            {
              "name": "Get_Full_Customer_Profile_for_cust-abc-987",
              "sourceSystemId": 1,
              "endpointId": 1,
              "pathParameterValues": {
                "customerId": "cust-abc-987"
              },
              "queryParameterValues": {},
              "headerValues": {},
              "responseDts": "{\\n  \\"profile\\": {\\n    \\"id\\": \\"cust-abc-987\\",\\n    \\"username\\": \\"johndoe\\",\\n    \\"registeredOn\\": \\"2023-01-15T10:00:00Z\\",\\n    \\"isActive\\": true,\\n    \\"tier\\": \\"Gold\\",\\n    \\"contact\\": {\\n      \\"email\\": \\"john.doe@example.com\\",\\n      \\"phone\\": null\\n    },\\n    \\"preferences\\": {\\n      \\"newsletter\\": true,\\n      \\"notifications\\": [\\n        \\"email\\",\\n        \\"sms\\"\\n      ]\\n    }\\n  },\\n  \\"recentOrders\\": [\\n    {\\n      \\"orderId\\": \\"ord-1122-3344\\",\\n      \\"orderDate\\": \\"2025-07-10T14:30:00Z\\",\\n      \\"totalAmount\\": 199.99,\\n      \\"isShipped\\": false,\\n      \\"items\\": [\\n        {\\n          \\"sku\\": \\"SKU-WDGT-001\\",\\n          \\"productName\\": \\"Super Widget\\",\\n          \\"quantity\\": 2,\\n          \\"price\\": 49.95\\n        },\\n        {\\n          \\"sku\\": \\"SKU-ACC-005\\",\\n          \\"productName\\": \\"Accessory Kit\\",\\n          \\"quantity\\": 1,\\n          \\"price\\": 99.99\\n        }\\n      ]\\n    },\\n    {\\n      \\"orderId\\": \\"ord-5566-7788\\",\\n      \\"orderDate\\": \\"2025-06-25T09:00:00Z\\",\\n      \\"totalAmount\\": 50.00,\\n      \\"isShipped\\": true,\\n      \\"items\\": [\\n        {\\n          \\"sku\\": \\"SKU-GFT-002\\",\\n          \\"productName\\": \\"Gift Card\\",\\n          \\"quantity\\": 1,\\n          \\"price\\": 50.00\\n        }\\n      ]\\n    }\\n  ],\\n  \\"shippingAddress\\": null,\\n  \\"stats\\": {\\n    \\"totalOrders\\": 27,\\n    \\"lifetimeValue\\": 4582.50\\n  }\\n}",
              "pollingIntervallTimeInMs": 60000,
              "active": true
            }
            """;
    public static final String VALID_EXAMPLE_ENDPOINT_CREATE = """
            [{
              "endpointPath": "/test/user",
              "httpRequestType": "GET"
            },
            {
              "endpointPath": "/test/car",
              "httpRequestType": "POST"
            }]
            """;
    public static final String VALID_QUERY_PARAM_CREATE = """
            {
              "paramName": "userId",
              "queryParamType": "QUERY",
              "values": ["123", "456", "789"]
            }""";

    private Examples() {

    }

    static final String VALID_EXAMPLE_SYNCJOB = """
            {
              "id": "1",
              "name": "Basyx sync job",
              "deployed": false
            }
            """;

    static final String VALID_EXAMPLE_SYNCJOB_TO_CREATE = """
            {
              "name": "Basic Userdata sync",
              "deployed": true
            }
            """;

    static final String VALID_SOURCE_SYSTEM_CREATE = """ 
            { 
              "name": "Dummy_Json", 
              "apiUrl": "https://dummyjson.com", 
              "description": "Some DummyData here", 
              "apiType": "REST", 
              "authConfig": { 
                "authType": "BASIC", 
                "username": "admin", 
                "password": "secretpassword123" 
              }, 
              "openApiSpec": "{ \\"openapi\\": \\"3.0.0\\", \\"info\\": { \\"title\\": \\"DummyJSON API\\", \\"version\\": \\"0.0.5\\", \\"description\\": \\"DummyJSON API\\" }, \\"externalDocs\\": { \\"description\\": \\"swagger.json\\", \\"url\\": \\"/api-docs/swagger.json\\" }, \\"servers\\": [], \\"security\\": [], \\"tags\\": [ { \\"name\\": \\"Product\\", \\"description\\": \\"The product API\\" } ], \\"paths\\": { \\"/products\\": { \\"get\\": { \\"summary\\": \\"get all products\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"query\\", \\"name\\": \\"limit\\", \\"schema\\": { \\"type\\": \\"number\\" }, \\"required\\": false }, { \\"in\\": \\"query\\", \\"name\\": \\"skip\\", \\"schema\\": { \\"type\\": \\"number\\" }, \\"required\\": false }, { \\"in\\": \\"query\\", \\"name\\": \\"select\\", \\"schema\\": { \\"type\\": \\"string\\" }, \\"required\\": false } ], \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Products\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } } }, \\"/products/search\\": { \\"get\\": { \\"summary\\": \\"search products\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"query\\", \\"name\\": \\"q\\", \\"description\\": \\"searchQuery\\", \\"schema\\": { \\"type\\": \\"string\\" }, \\"required\\": false }, { \\"in\\": \\"query\\", \\"name\\": \\"limit\\", \\"schema\\": { \\"type\\": \\"number\\" }, \\"required\\": false }, { \\"in\\": \\"query\\", \\"name\\": \\"skip\\", \\"schema\\": { \\"type\\": \\"number\\" }, \\"required\\": false }, { \\"in\\": \\"query\\", \\"name\\": \\"select\\", \\"schema\\": { \\"type\\": \\"string\\" }, \\"required\\": false } ], \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Products\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } } }, \\"/products/categories\\": { \\"get\\": { \\"summary\\": \\"get all products categories\\", \\"tags\\": [\\"Product\\"], \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"type\\": \\"array\\", \\"items\\": { \\"type\\": \\"string\\" } } } } } } } }, \\"/products/category/{category_name}\\": { \\"get\\": { \\"summary\\": \\"get products of category\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"path\\", \\"name\\": \\"category_name\\", \\"description\\": \\"categorName\\", \\"schema\\": { \\"type\\": \\"string\\" }, \\"required\\": true }, { \\"in\\": \\"query\\", \\"name\\": \\"select\\", \\"schema\\": { \\"type\\": \\"string\\" }, \\"required\\": false } ], \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Products\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } } }, \\"/products/add\\": { \\"post\\": { \\"summary\\": \\"create a new product\\", \\"tags\\": [\\"Product\\"], \\"requestBody\\": { \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } } }, \\"/products/{product_id}\\": { \\"get\\": { \\"summary\\": \\"get product by id\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"path\\", \\"name\\": \\"product_id\\", \\"schema\\": { \\"type\\": \\"integer\\" }, \\"required\\": true }, { \\"in\\": \\"query\\", \\"name\\": \\"select\\", \\"schema\\": { \\"type\\": \\"string\\" }, \\"required\\": false } ], \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } }, \\"put\\": { \\"summary\\": \\"update a product\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"path\\", \\"name\\": \\"product_id\\", \\"schema\\": { \\"type\\": \\"integer\\" }, \\"required\\": true } ], \\"requestBody\\": { \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } }, \\"patch\\": { \\"summary\\": \\"update a product\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"path\\", \\"name\\": \\"product_id\\", \\"schema\\": { \\"type\\": \\"integer\\" }, \\"required\\": true } ], \\"requestBody\\": { \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } }, \\"delete\\": { \\"summary\\": \\"delete a product\\", \\"tags\\": [\\"Product\\"], \\"parameters\\": [ { \\"in\\": \\"path\\", \\"name\\": \\"product_id\\", \\"schema\\": { \\"type\\": \\"integer\\" }, \\"required\\": true } ], \\"responses\\": { \\"200\\": { \\"description\\": \\"success\\", \\"content\\": { \\"application/json\\": { \\"schema\\": { \\"$ref\\": \\"#/components/schemas/ProductDelete\\" } } } }, \\"500\\": { \\"description\\": \\"error\\" } } } } }, \\"components\\": { \\"schemas\\": { \\"Products\\": { \\"type\\": \\"object\\", \\"properties\\": { \\"products\\": { \\"type\\": \\"array\\", \\"items\\": { \\"$ref\\": \\"#/components/schemas/Product\\" } }, \\"total\\": { \\"type\\": \\"number\\" }, \\"skip\\": { \\"type\\": \\"number\\" }, \\"limit\\": { \\"type\\": \\"number\\" } } }, \\"Product\\": { \\"type\\": \\"object\\", \\"properties\\": { \\"id\\": { \\"type\\": \\"number\\" }, \\"title\\": { \\"type\\": \\"string\\" }, \\"description\\": { \\"type\\": \\"string\\" }, \\"price\\": { \\"type\\": \\"number\\" }, \\"discountPercentage\\": { \\"type\\": \\"number\\" }, \\"rating\\": { \\"type\\": \\"number\\" }, \\"stock\\": { \\"type\\": \\"number\\" }, \\"brand\\": { \\"type\\": \\"string\\" }, \\"category\\": { \\"type\\": \\"string\\" }, \\"thumbnail\\": { \\"type\\": \\"string\\" }, \\"images\\": { \\"type\\": \\"array\\", \\"items\\": { \\"type\\": \\"string\\" } } } }, \\"ProductDelete\\": { \\"allOf\\": [ { \\"$ref\\": \\"#/components/schemas/Product\\" }, { \\"type\\": \\"object\\", \\"properties\\": { \\"isDeleted\\": { \\"type\\": \\"boolean\\" }, \\"deletedOn\\": { \\"type\\": \\"string\\" } } } ] } } }, \\"webhooks\\": {} }" 
            } 
            """;

    static final String VALID_SOURCE_SYSTEM_ENDPOINT_POST = """
            {
              "endpointPath": "/test",
              "httpRequestType": "GET"
            }
            """;

}
