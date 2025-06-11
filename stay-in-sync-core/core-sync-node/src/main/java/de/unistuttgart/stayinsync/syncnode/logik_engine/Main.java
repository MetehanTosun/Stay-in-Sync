// Datei: Main.java
package de.unistuttgart.stayinsync.syncnode.logik_engine; // Dein Package

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.ArrayList;
import java.util.HashMap; // Nur falls du die Map-basierte ConstantNode-Variante doch testest
import java.util.List;
import java.util.Map;   // Nur falls du die Map-basierte ConstantNode-Variante doch testest

public class Main {

    public static void main(String[] args) {
        // 0. Erstelle den Evaluator (wird für alle Tests wiederverwendet)
        LogicGraphEvaluator evaluator = new LogicGraphEvaluator();

        System.out.println("=== LOGIC ENGINE TEST SUITE START ===");

        // 1. Erstelle das JsonObject, das als Quelle für JsonNode-Instanzen dient
        // Dieses Objekt wird jetzt direkt an die JsonNode-Instanzen übergeben.
        JsonObjectBuilder rootJsonBuilder = Json.createObjectBuilder();
        rootJsonBuilder.add("sensorData", Json.createObjectBuilder()
                .add("currentTemperature", 25.5)
                .add("pressure", 1012.5)
                .add("humidity", 60.0)
        );
        rootJsonBuilder.add("thresholds", Json.createObjectBuilder()
                .add("maxAllowedTemp", 30.0)
                .add("minPressureForOperation", 1000.0)
        );
        rootJsonBuilder.add("config", Json.createObjectBuilder()
                .add("isSystemEnabled", true)
                .add("operationalMode", "AUTO")
        );
        JsonObject aasDataSource = rootJsonBuilder.build();


        // --- Testfall 1: Komplexer Graph mit verschiedenen Input-Typen ---
        // Logik:
        // 1. correctedTemp = AAS_Temperature + Constant_Offset
        // 2. isTempOk = correctedTemp < AAS_MaxAllowedTemp
        // 3. isPressureOk = AAS_Pressure > AAS_MinPressure
        // 4. isSystemReady = isTempOk AND isPressureOk AND AAS_SystemEnabled
        //
        // Erwartetes Ergebnis für diesen Testfall:
        // correctedTemp = 25.5 + (-2.0) = 23.5
        // isTempOk = 23.5 < 30.0 => true
        // isPressureOk = 1012.5 > 1000.0 => true
        // isSystemReady = true AND true AND true => true

        System.out.println("\n--- Testfall 1: Komplexer Graph ---");
        try {
            // Input-Definitionen
            JsonNode aasTemp = new JsonNode(aasDataSource, "sensorData.currentTemperature");          // 25.5
            ConstantNode tempOffset = new ConstantNode("TemperatureOffset", -2.0);                   // -2.0
            JsonNode aasMaxTemp = new JsonNode(aasDataSource, "thresholds.maxAllowedTemp");            // 30.0
            JsonNode aasPressure = new JsonNode(aasDataSource, "sensorData.pressure");                 // 1012.5
            JsonNode aasMinPressure = new JsonNode(aasDataSource, "thresholds.minPressureForOperation"); // 1000.0
            JsonNode aasSystemEnabled = new JsonNode(aasDataSource, "config.isSystemEnabled");         // true

            // Knoten erstellen
            LogicNode correctedTempNode = new LogicNode(
                    "CorrectedTemperature",
                    LogicOperator.ADD,
                    aasTemp,
                    tempOffset
            );

            LogicNode tempOkNode = new LogicNode(
                    "IsTemperatureOk",
                    LogicOperator.LESS_THAN,
                    new ParentNode(correctedTempNode),
                    aasMaxTemp
            );

            LogicNode pressureOkNode = new LogicNode(
                    "IsPressureOk",
                    LogicOperator.GREATER_THAN,
                    aasPressure,
                    aasMinPressure
            );

            LogicNode systemReadyNode = new LogicNode(
                    "IsSystemReady", // Dieser ist der implizite Zielknoten, da er keine Kinder hat
                    LogicOperator.AND,
                    new ParentNode(tempOkNode),
                    new ParentNode(pressureOkNode),
                    aasSystemEnabled
            );

            // Graph-Liste erstellen
            List<LogicNode> graphNodes = new ArrayList<>();
            graphNodes.add(correctedTempNode);
            graphNodes.add(tempOkNode);
            graphNodes.add(pressureOkNode);
            graphNodes.add(systemReadyNode);

            boolean finalResult = evaluator.evaluateGraph(graphNodes);

            System.out.println("Ergebnis des Graphen ('IsSystemReady'): " + finalResult);
            if (finalResult) { // Erwartet true
                System.out.println("Testfall 1: ERFOLGREICH!");
            } else {
                System.out.println("Testfall 1: FEHLGESCHLAGEN! Erwartet: true, Erhalten: " + finalResult);
            }

            // Optional: Ergebnisse von Zwischenknoten ausgeben (wenn sie im LogicNode gespeichert werden)
            System.out.println("  Zwischenergebnis 'CorrectedTemperature': " + correctedTempNode.getCalculatedResult());
            System.out.println("  Zwischenergebnis 'IsTemperatureOk': " + tempOkNode.getCalculatedResult());
            System.out.println("  Zwischenergebnis 'IsPressureOk': " + pressureOkNode.getCalculatedResult());


        } catch (Exception e) { // Fange allgemein Exceptions für den Test
            System.err.println("Fehler in Testfall 1: " + e.getMessage());
            e.printStackTrace();
        }

        // --- Testfall 2: Ein Graph, der 'false' ergeben sollte ---
        // Logik: AAS_Humidity > Constant_Max_Humidity (60.0 > 50.0 => true)
        //        UND
        //        AAS_System_Enabled (true)
        //        ==> true (aber wir wollen einen False-Fall provozieren)
        // Ändern wir es zu: (AAS_Humidity < Constant_Min_Humidity)
        // (60.0 < 50.0) => false

        System.out.println("\n--- Testfall 2: Graph mit False-Ergebnis ---");
        try {
            JsonNode aasHumidity = new JsonNode(aasDataSource, "sensorData.humidity"); // 60.0
            ConstantNode minHumidityThreshold = new ConstantNode("MinHumidityThreshold", 50.0);

            LogicNode humidityCheckNode = new LogicNode(
                    "HumidityBelowMin",
                    LogicOperator.LESS_THAN,
                    aasHumidity,
                    minHumidityThreshold
            );

            List<LogicNode> graphNodes2 = new ArrayList<>();
            graphNodes2.add(humidityCheckNode);

            Map<String, Object> emptyUiValues2 = new HashMap<>();
            boolean finalResult2 = evaluator.evaluateGraph(graphNodes2);

            System.out.println("Ergebnis des Graphen ('HumidityBelowMin'): " + finalResult2);
            if (!finalResult2) { // Erwartet false
                System.out.println("Testfall 2: ERFOLGREICH!");
            } else {
                System.out.println("Testfall 2: FEHLGESCHLAGEN! Erwartet: false, Erhalten: " + finalResult2);
            }

        } catch (Exception e) {
            System.err.println("Fehler in Testfall 2: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== LOGIC ENGINE TEST SUITE END ===");
    }
}