import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Template } from '../models/template.model';

@Injectable({
  providedIn: 'root'
})
export class TemplateService {
  private localStorageKey = 'edc-templates';

  private getDefaultTemplates(): Template[] {
    // The original hardcoded templates, now with a 'type'
    return [
      {
        id: `template-${this.generateUuid()}`,
        name: 'Default BPN Access Policy',
        description: 'A simple policy to grant access based on a Business Partner Number.',
        type: 'AccessPolicy',
        content: {
          "@context": {"odrl": "http://www.w3.org/ns/odrl/2/"},
          "@id": "POLICY_ID_BPN",
          "policy": {
            "permission": [{
              "action": "${Action|use,read,write}",
              "constraint": [{
                "leftOperand": "BusinessPartnerNumber",
                "operator": "${Operator|eq,neq}",
                "rightOperand": "${BPN-Value}"
              }]
            }]
          }
        }
      },
      {
        id: `template-${this.generateUuid()}`,
        name: 'CX Membership Policy',
        description: 'A Catena-X policy that checks for active membership and a BPN.',
        type: 'AccessPolicy',
        content: {
          "@context": [
            "http://www.w3.org/ns/odrl.jsonld",
            "https://w3id.org/catenax/2025/9/policy/context.jsonld"
          ],
          "@type": "Set",
          "@id": "POLICY_ID_BPN",
          "permission": [
            {
              "action": "${Action|access,read,write,use}",
              "constraint": [
                {
                  "and": [
                    {
                      "leftOperand": "Membership",
                      "operator": "${Operator|eq,neq}",
                      "rightOperand": "${Status|active,inactive}"
                    },
                    {
                      "leftOperand": "BusinessPartnerNumber",
                      "operator": "${Operator|isAnyOf,eq,neq}",
                      "rightOperand": [
                        "${BPN-Value}"
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      },
    ];
  }

  getTemplates(): Observable<Template[]> {
    const templatesJson = localStorage.getItem(this.localStorageKey);
    if (templatesJson) {
      return of(JSON.parse(templatesJson));
    } else {
      const defaultTemplates = this.getDefaultTemplates();
      this.saveTemplates(defaultTemplates);
      return of(defaultTemplates);
    }
  }

  saveTemplates(templates: Template[]): Observable<Template[]> {
    localStorage.setItem(this.localStorageKey, JSON.stringify(templates));
    return of(templates);
  }

  private generateUuid(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}