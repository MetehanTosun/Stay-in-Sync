package de.unistuttgart.stayinsync.scriptengine;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ScriptEngineApp {

    public static void main(String ... args) {
        System.out.println("Running StayInSync Quarkus application...");
        Quarkus.run(args);
    }
}
