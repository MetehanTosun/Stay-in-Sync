export interface TransformationScriptDTO {
  id: number;
  name: string;
  typescriptCode: string;
  javascriptCode: string;
  requiredArcAliases: string[];
  status: string; // or a proper enum if you want
  targetArcIds: number[];
  generatedSdkCode: string;
}
