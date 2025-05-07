package de.unistuttgart.stayinsync.scriptengine;

import java.util.HashMap;
import java.util.Map;

public class SyncJobFactory {

    public static ScriptEngineService.SyncJob getIncrementByOneJob(String scriptId, String mockedHash, Number input) {
        String scriptCode = """
                const input = stayinsync.getInput();
                if(typeof input === 'number'){
                    stayinsync.setOutput(input + 1);
                } else {
                    stayinsync.setOutput(null);
                }
                """;

        return new ScriptEngineService.SyncJob(
                scriptId,
                scriptCode,
                mockedHash,
                input
        );
    }

    public static ScriptEngineService.SyncJob getMultiplyByConstantJob(String scriptId, String mockedHash, Number input, int constant) {
        String scriptCode = String.format("""
                const input = stayinsync.getInput();
                if(typeof input === 'number'){
                    stayinsync.setOutput(input * %d);
                } else {
                    stayinsync.setOutput(null);
                }
                """, constant);

        return new ScriptEngineService.SyncJob(
                scriptId,
                scriptCode,
                mockedHash,
                input
        );
    }

    public static ScriptEngineService.SyncJob getSquaredJob(String scriptId, String mockedHash, Number input) {
        String scriptCode = """
                const input = stayinsync.getInput();
                if(typeof input === 'number'){
                    stayinsync.setOutput(input * input);
                } else {
                    stayinsync.setOutput(null);
                }
                """;

        return new ScriptEngineService.SyncJob(
                scriptId,
                scriptCode,
                mockedHash,
                input
        );
    }

    public static ScriptEngineService.SyncJob getConcatenationToHelloJob(String scriptId, String mockedHash, String suffix) {
        String scriptCode = """
                const suffix = stayinsync.getInput();
                if(typeof suffix === 'string'){
                    stayinsync.setOutput('Hello ' + suffix);
                } else {
                    stayinsync.setOutput('Hello nothing.');
                }
                """;

        return new ScriptEngineService.SyncJob(
                scriptId,
                scriptCode,
                mockedHash,
                suffix
        );
    }

    public static ScriptEngineService.SyncJob getJSONMockTransformationJobTwoNamespaces(String scriptId, String mockedHash, Map<String, Object> managementData, Map<String, Object> manufacturingData) {
        String scriptCode = """
                let outputObject = {
                    error: "Input namespaces not found or incomplete."
                };
                
                if (typeof management !== 'undefined' && typeof manufacturing !== 'undefined') {
                    const facility = management.facilityName || 'Unknown Facility';
                    const operatorId = management.operatorId || 'N/A'; // Operator ID from management
                
                    const productId = manufacturing.productId || 'Unknown Product';
                    // If you needed an operatorId specifically from manufacturing for another purpose:
                    // const manufacturingOperatorId = manufacturing.operatorId || 'N/A_MFG';
                    const batchSize = manufacturing.batchSize || 0; // Declare and get batchSize
                    const criticalValue = (typeof manufacturing.criticalValue === 'number') ? manufacturing.criticalValue : 0;
                
                    outputObject = {
                        aasIdentifier: "AAS_FOR_" + facility + "_" + productId,
                        dataPayload: {
                            managedBy: operatorId, // Uses operatorId from management
                            productReference: productId,
                            currentBatch: batchSize, // Use declared batchSize
                            computedCritical: criticalValue * 2,
                            managementNotes: management.notes, // Will be undefined if not present, which is fine for JS objects
                            manufacturingStatus: manufacturing.status // Will be undefined if not present
                        },
                        sourceTimestamp: new Date().toISOString() // Corrected method name
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

        return new ScriptEngineService.SyncJob(
                scriptId,
                scriptCode,
                mockedHash,
                jobInputData
        );
    }
}
