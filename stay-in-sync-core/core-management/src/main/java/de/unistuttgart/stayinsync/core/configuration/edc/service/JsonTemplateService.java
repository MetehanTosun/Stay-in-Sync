package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.client.EDCClient;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.JsonTemplateDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.JsonTemplate;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.JsonTemplateMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JsonTemplateService {

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    EDCClient edcClient;


    public JsonTemplateDto fetchJsonTemplateFromDatabase(final UUID id) throws EntityNotFoundException{
        final JsonTemplate jsonTemplate = JsonTemplate.findById(id);
        if(jsonTemplate == null){
            final String errorMessage = "No Json-Template found with given id.";
            Log.errorf(errorMessage, id);
            throw new EntityNotFoundException(errorMessage);
        }
        return JsonTemplateMapper.templateMapper.templateToTemplateDto(jsonTemplate);
    }



    public List<JsonTemplateDto> fetchAllTemplates(){
        List<JsonTemplateDto> jsonTemplateDtos = new ArrayList<>();
        for(JsonTemplate jsonTemplate : JsonTemplate.<JsonTemplate>listAll()){
            jsonTemplateDtos.add(JsonTemplateMapper.templateMapper.templateToTemplateDto(jsonTemplate));
        }
        return jsonTemplateDtos;
    }

    @Transactional
    public JsonTemplateDto persistJsonTemplate (JsonTemplateDto jsonTemplateDto){
        final JsonTemplate jsonTemplate = JsonTemplateMapper.templateMapper.templateDtoToTemplate(jsonTemplateDto);
        jsonTemplate.persist();
        return JsonTemplateMapper.templateMapper.templateToTemplateDto(jsonTemplate);
    }

    @Transactional
    public JsonTemplateDto update (final UUID id, final JsonTemplateDto updatedJsonTemplateDto){
        final JsonTemplate persistedJsonTemplate = JsonTemplate.findById(id);
        final JsonTemplate updatedJsonTemplate = JsonTemplateMapper.templateMapper.templateDtoToTemplate(updatedJsonTemplateDto);

        if(persistedJsonTemplate == null){
            final String errorMessage = "No Json-Template found with given id.";
            Log.errorf(errorMessage, id);
            throw new EntityNotFoundException(errorMessage);
        }

        persistedJsonTemplate.setContent(updatedJsonTemplate.getContent());
        persistedJsonTemplate.setName(updatedJsonTemplate.getName());
        persistedJsonTemplate.setDescription(updatedJsonTemplate.getDescription());

        return JsonTemplateMapper.templateMapper.templateToTemplateDto(persistedJsonTemplate);
    }

    @Transactional
    public boolean removeJsonTemplateFromDatabase(final UUID id){
        if(JsonTemplate.findById(id) == null){
            final String errorMessage = "No Json-Template found with given id.";
            Log.errorf(errorMessage, id);
            throw new EntityNotFoundException(errorMessage);
        }

        return JsonTemplate.deleteById(id);
    }




}
