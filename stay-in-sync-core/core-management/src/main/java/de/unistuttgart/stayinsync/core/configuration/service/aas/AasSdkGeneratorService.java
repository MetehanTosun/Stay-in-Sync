package de.unistuttgart.stayinsync.core.configuration.service.aas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasTargetApiRequestConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AasSdkGeneratorService {

    private record ElementJson(String idShort, String idShortPath, String parentPath, String modelType) {}

    @Inject
    ObjectMapper objectMapper;

    public String generateSdkForAasArc(AasTargetApiRequestConfiguration arc) throws JsonProcessingException {
        List<AasElementLite> elements = AasElementLite.list("submodelLite", arc.submodel);

        String elementsJson = objectMapper.writeValueAsString(elements.stream()
                .map(e -> new ElementJson(e.idShort, e.idShortPath, e.parentPath, e.modelType))
                .collect(Collectors.toList()));

        String arcAlias = arc.alias;

        // The generated JavaScript is a self-contained module (IIFE)
        return String.format("""
            (function() {
              'use strict';
              const arcAlias = '%s';
              const elements = %s;

              /**
               * A helper function to build a nested object structure from a flat list of elements with path properties.
               */
              function buildNestedObject(elements) {
                const root = {};
                const map = {};

                elements.forEach(el => {
                  map[el.idShortPath] = el;
                  el.children = {}; // Prepare a children map for every element
                });

                elements.forEach(el => {
                  if (!el.parentPath || el.parentPath === '') {
                    root[el.idShort] = el.children;
                  } else {
                    const parent = map[el.parentPath];
                    if (parent) {
                      parent.children[el.idShort] = el.children;
                    }
                  }

                  // If the element is a "Property", attach the action methods (setValue, etc.)
                  if (el.modelType === 'Property') {
                     attachPropertyActions(el.children, el.idShortPath);
                  }
                });
                return root;
              }

              /**
               * Attaches the .setValue() and other action methods to a leaf node (a Property).
               */
              function attachPropertyActions(targetObject, idShortPath) {
                targetObject.setValue = function(value) {
                  return {
                    build: function() {
                      return {
                        __directiveType: 'AasUpdateValueDirective',
                        arcAlias: arcAlias,
                        elementIdShortPath: idShortPath,
                        value: value
                      };
                    }
                  };
                };

                targetObject.delete = function() {
                   return {
                     build: function() {
                       return {
                         __directiveType: 'AasDeleteElementDirective',
                         arcAlias: arcAlias,
                         elementIdShortPath: idShortPath
                       };
                     }
                   };
                };
              }

              targets[arcAlias] = buildNestedObject(elements);
            })();
            """, arcAlias, elementsJson);
    }
}
