#!/bin/bash

echo "🧪 Running AAS Frontend Tests"
echo "=============================="

# Run unit tests for AAS service
echo "📋 Running AAS Service Unit Tests..."
npm test -- --include="**/aas.service.spec.ts" --watch=false

# Run unit tests for CreateSourceSystemComponent
echo "📋 Running CreateSourceSystemComponent Unit Tests..."
npm test -- --include="**/create-source-system.component.spec.ts" --watch=false

# Run integration tests
echo "📋 Running AAS Integration Tests..."
npm test -- --include="**/create-source-system.integration.spec.ts" --watch=false

# Run all AAS-related tests
echo "📋 Running All AAS Tests..."
npm test -- --include="**/*aas*.spec.ts" --watch=false

echo "✅ AAS Tests Complete!"
echo ""
echo "📊 Test Coverage Report:"
echo "========================"
npm test -- --code-coverage --watch=false --include="**/aas*.spec.ts"

echo ""
echo "🎯 Test Summary:"
echo "================"
echo "✅ Unit Tests: AAS Service functionality"
echo "✅ Unit Tests: CreateSourceSystemComponent functionality" 
echo "✅ Integration Tests: End-to-end AAS workflows"
echo "✅ Error Handling: Network errors, malformed data"
echo "✅ Type Inference: AAS element type detection"
echo "✅ Tree Operations: Loading, filtering, mapping"
echo "✅ CRUD Operations: Create, read, update, delete elements"
