/**
 * Represents a transformation script configuration entity.
 *
 * This Data Transfer Object (DTO) holds the TypeScript/JavaScript source code
 * and metadata for a transformation. It defines how data is processed within
 * the StayInSync system and provides information about its dependencies and status.
 *
 * Fields overview:
 * - {@link id}: Unique numeric identifier of the transformation script.
 * - {@link name}: Human-readable name for identification.
 * - {@link typescriptCode}: Original TypeScript source code of the transformation.
 * - {@link javascriptCode}: Transpiled JavaScript version executed in GraalJS.
 * - {@link requiredArcAliases}: Array of aliases representing dependent arcs.
 * - {@link status}: Current state of the script (e.g., active, draft, disabled).
 * - {@link targetArcIds}: Identifiers of target arcs this transformation affects.
 * - {@link generatedSdkCode}: Auto-generated SDK snippet injected during replay to support targets.* API usage.
 *
 * @author Mohammed-Ammar Hassnou
 */
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
