// Datei: CreateSourceSystemJsonDTO.java
package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateSourceSystemJsonDTO {

  @NotBlank public String name;
  public String description;
  @NotNull public SourceSystemType type;
  @NotBlank public String apiUrl;
  public AuthType authType;
  public String username;
  public String password;
  public String apiKey;
  public String openApiSpecUrl;
  public String openApiSpec;  // optional raw spec

}