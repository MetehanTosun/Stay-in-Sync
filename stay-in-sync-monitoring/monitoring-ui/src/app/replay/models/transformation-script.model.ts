export interface TransformationScriptDTO {
  id: number; // Long in Java → number in TS
  name: string;
  typescriptCode: string; // ← we will use this
  javascriptCode: string;
}
