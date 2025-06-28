// Datei: de/unistuttgart/stayinsync/core/configuration/rest/dtos/CreateSourceSystemForm.java
package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Formular für Multipart-POST /api/source-systems/upload
 */
public class CreateSourceSystemForm {

    @RestForm("name")
    public String name;

    @RestForm("description")
    public String description;

    @RestForm("type")
    public SourceSystemType type;

    @RestForm("apiUrl")
    public String apiUrl;

    @RestForm("authType")
    public AuthType authType;

    @RestForm("username")
    public String username;

    @RestForm("password")
    public String password;

    @RestForm("apiKey")
    public String apiKey;

    @RestForm("openApiSpecUrl")
    public String openApiSpecUrl;

    @RestForm("file")
    public FileUpload file;

    /**
     * Liest die hochgeladene Datei in einen Byte-Array.
     */
    public byte[] uploadedFileBytes() throws IOException {
        Path p = file.uploadedFile();
        return Files.readAllBytes(p);
    }
}