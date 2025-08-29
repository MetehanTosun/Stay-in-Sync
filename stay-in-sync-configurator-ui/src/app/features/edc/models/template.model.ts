export type TemplateType = 'AccessPolicy' | 'Asset' | 'ContractDefinition';

export interface Template {
  id: string;
  name: string;
  description: string;
  type: TemplateType;
  content: any;
}