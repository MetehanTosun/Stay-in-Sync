package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;

import java.util.HashMap;
import java.util.Map;

public class SyncJobFactory {

    public static TransformJob getIncrementByOneJob(String scriptId, String mockedHash, Number input) {
        String scriptCode = """
                const input = stayinsync.getInput();
                
                if(typeof input === 'number'){
                    stayinsync.setOutput(input + 1);
                } else {
                    stayinsync.setOutput(null);
                }
                """;

        return new TransformJob(
                "incByOne",
                "Test1",
                scriptId,
                scriptCode,
                "js",
                mockedHash,
                input
        );
    }

    public static TransformJob getMultiplyByConstantJob(String scriptId, String mockedHash, Number input, int constant) {
        String scriptCode = String.format("""
                const input = stayinsync.getInput();
                if(typeof input === 'number'){
                    stayinsync.setOutput(input * %d);
                } else {
                    stayinsync.setOutput(null);
                }
                """, constant);

        return new TransformJob(
                "multByConst",
                "Test2",
                scriptId,
                scriptCode,
                "js",
                mockedHash,
                input
        );
    }

    public static TransformJob getSquaredJob(String scriptId, String mockedHash, Number input) {
        String scriptCode = """
                const input = stayinsync.getInput();
                if(typeof input === 'number'){
                    stayinsync.setOutput(input * input);
                } else {
                    stayinsync.setOutput(null);
                }
                """;

        return new TransformJob(
                "squared",
                "Test3",
                scriptId,
                scriptCode,
                "js",
                mockedHash,
                input
        );
    }

    public static TransformJob getConcatenationToHelloJob(String scriptId, String mockedHash, String suffix) {
        String scriptCode = """
                const suffix = stayinsync.getInput();
                if(typeof suffix === 'string'){
                    stayinsync.setOutput('Hello ' + suffix);
                } else {
                    stayinsync.setOutput('Hello nothing.');
                }
                """;

        return new TransformJob(
                "concact",
                "Test4",
                scriptId,
                scriptCode,
                "js",
                mockedHash,
                suffix
        );
    }

    public static TransformJob getJSONMockTransformationJobTwoNamespaces(String scriptId, String mockedHash, Map<String, Object> managementData, Map<String, Object> manufacturingData) {
        String scriptCode = """
                let outputObject = {
                    error: "Input namespaces not found or incomplete."
                };
                const input = stayinsync.getInput();
                const management = input.management;
                const manufacturing = input.manufacturing;
                
                if (typeof management !== 'undefined' && typeof manufacturing !== 'undefined') {
                    const facility = management.facilityName || 'Unknown Facility';
                    const operatorId = management.operatorId || 'N/A';
                
                    const productId = manufacturing.productId || 'Unknown Product';
                
                    const batchSize = manufacturing.batchSize || 0;
                    const criticalValue = (typeof manufacturing.criticalValue === 'number') ? manufacturing.criticalValue : 0;
                
                    outputObject = {
                        aasIdentifier: "AAS_FOR_" + facility + "_" + productId,
                        dataPayload: {
                            managedBy: operatorId,
                            productReference: productId,
                            currentBatch: batchSize,
                            computedCritical: criticalValue * 2,
                            managementNotes: management.notes,
                            manufacturingStatus: manufacturing.status,
                            eqResult: eval(management.equation),
                        },
                        sourceTimestamp: new Date().toISOString()
                    };
                } else {
                    if (typeof management === 'undefined') stayinsync.log("Management data namespace not found!", "ERROR");
                    if (typeof manufacturing === 'undefined') stayinsync.log("Manufacturing data namespace not found!", "ERROR");
                }
                
                stayinsync.setOutput(outputObject);
                """;

        Map<String, Object> jobInputData = new HashMap<>();
        jobInputData.put("management", managementData);
        jobInputData.put("manufacturing", manufacturingData);

        return new TransformJob(
                "twoObjTransform",
                "Test5",
                scriptId,
                scriptCode,
                "js",
                mockedHash,
                jobInputData
        );
    }
}
