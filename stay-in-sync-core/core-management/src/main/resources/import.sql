-- General Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('EXISTS', 'GENERAL', 'Checks if one or more JSON paths exist.', '["PROVIDER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT_EXISTS', 'GENERAL', 'Checks if one or more JSON paths do not exist.', '["PROVIDER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('IS_NULL', 'GENERAL', 'Checks if a value is explicitly null.', '["ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('IS_NOT_NULL', 'GENERAL', 'Checks if a value exists and is not null.', '["ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('TYPE_IS', 'GENERAL', 'Checks the Java type of a value.', '["STRING", "ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('EQUALS', 'GENERAL', 'Checks if two or more values are equal.', '["ANY", "ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT_EQUALS', 'GENERAL', 'Checks if two or more values are not equal.', '["ANY", "ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('IN_SET', 'GENERAL', 'Checks if a value is present in a set of values.', '["ANY", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT_IN_SET', 'GENERAL', 'Checks if a value is not present in a set of values.', '["ANY", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('ONE_OF', 'GENERAL', 'Succeeds if at least one input is true (predicative).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('ALL_OF', 'GENERAL', 'Succeeds if all inputs are true (predicative).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NONE_OF', 'GENERAL', 'Succeeds if no input is true (predicative).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('AND', 'GENERAL', 'Performs a strict logical AND operation.', '["BOOLEAN", "BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('OR', 'GENERAL', 'Performs a strict logical OR operation.', '["BOOLEAN", "BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('XOR', 'GENERAL', 'Checks if exactly one input is true (strict).', '["BOOLEAN", "BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT', 'GENERAL', 'Negates a single boolean value (strict).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('MATCHES_SCHEMA', 'GENERAL', 'Validates a JSON document against a JSON schema.', '["ANY", "JSON"]', 'BOOLEAN');

-- Number Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('GREATER_THAN', 'NUMBER', 'Checks if a number is greater than another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('LESS_THAN', 'NUMBER', 'Checks if a number is less than another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('GREATER_OR_EQUAL', 'NUMBER', 'Checks if a number is greater than or equal to another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('LESS_OR_EQUAL', 'NUMBER', 'Checks if a number is less than or equal to another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('BETWEEN', 'NUMBER', 'Checks if a number is between two bounds (inclusive).', '["NUMBER", "NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT_BETWEEN', 'NUMBER', 'Checks if a number is outside of two bounds.', '["NUMBER", "NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('ADD', 'NUMBER', 'Adds two or more numbers.', '["NUMBER", "NUMBER"]', 'NUMBER');

-- Array/List Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('LENGTH_EQUALS', 'ARRAY', 'Compares the length of a list with a number.', '["ARRAY", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('LENGTH_GT', 'ARRAY', 'Checks if the length of a list is greater than a number.', '["ARRAY", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('LENGTH_LT', 'ARRAY', 'Checks if the length of a list is less than a number.', '["ARRAY", "NUMBER"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT_EMPTY', 'ARRAY', 'Checks if a list or array is not empty.', '["ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('CONTAINS_ELEMENT', 'ARRAY', 'Checks if an element is present in a list.', '["ARRAY", "ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('NOT_CONTAINS_ELEMENT', 'ARRAY', 'Checks if an element is not present in a list.', '["ARRAY", "ANY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('CONTAINS_ALL', 'ARRAY', 'Checks if a list contains all elements from another list.', '["ARRAY", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('CONTAINS_ANY', 'ARRAY', 'Checks if a list contains at least one element from another list.', '["ARRAY", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('CONTAINS_NONE', 'ARRAY', 'Checks if a list contains no elements from another list.', '["ARRAY", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('SUM', 'ARRAY', 'Calculates the sum of all numbers in a list.', '["ARRAY"]', 'NUMBER');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('AVG', 'ARRAY', 'Calculates the average of all numbers in a list.', '["ARRAY"]', 'NUMBER');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('MIN', 'ARRAY', 'Finds the smallest number in a list.', '["ARRAY"]', 'NUMBER');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('MAX', 'ARRAY', 'Finds the largest number in a list.', '["ARRAY"]', 'NUMBER');

-- Object Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('HAS_KEY', 'OBJECT', 'Checks if a JSON object has a specific key.', '["JSON", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('LACKS_KEY', 'OBJECT', 'Checks if a JSON object lacks a specific key.', '["JSON", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('HAS_ALL_KEYS', 'OBJECT', 'Checks if a JSON object has all specified keys.', '["JSON", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('HAS_ANY_KEY', 'OBJECT', 'Checks if a JSON object has at least one of the specified keys.', '["JSON", "ARRAY"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('HAS_NO_KEYS', 'OBJECT', 'Checks if a JSON object has none of the specified keys.', '["JSON", "ARRAY"]', 'BOOLEAN');

-- String Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('EQUALS_CASE_SENSITIVE', 'STRING', 'Performs a case-sensitive comparison of two strings.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('EQUALS_IGNORE_CASE', 'STRING', 'Performs a case-insensitive comparison of two strings.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('STRING_CONTAINS', 'STRING', 'Checks if a string contains another string.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('STRING_NOT_CONTAINS', 'STRING', 'Checks if a string does not contain another string.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('STRING_STARTS_WITH', 'STRING', 'Checks if a string starts with a specific prefix.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('STRING_ENDS_WITH', 'STRING', 'Checks if a string ends with a specific suffix.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('REGEX_MATCH', 'STRING', 'Checks if a string matches a regular expression pattern.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('STRING_LENGTH_BETWEEN', 'STRING', 'Checks if a string''s length is between two numbers.', '["STRING", "NUMBER", "NUMBER"]', 'BOOLEAN');

-- Date/Time Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('BEFORE', 'DATETIME', 'Checks if a date is before another date.', '["DATE", "DATE"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('AFTER', 'DATETIME', 'Checks if a date is after another date.', '["DATE", "DATE"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('BETWEEN_DATES', 'DATETIME', 'Checks if a date is between two other dates.', '["DATE", "DATE", "DATE"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('SAME_DAY', 'DATETIME', 'Checks if two dates are on the same calendar day.', '["DATE", "DATE"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('AGE_GREATER_THAN', 'DATETIME', 'Checks if a date is older than a specific duration.', '["DATE", "NUMBER", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('WITHIN_LAST', 'DATETIME', 'Checks if a date is within the last specified duration.', '["DATE", "NUMBER", "STRING"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('WITHIN_NEXT', 'DATETIME', 'Checks if a date is within the next specified duration.', '["DATE", "NUMBER", "STRING"]', 'BOOLEAN');

-- Boolean Predicates
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('IS_TRUE', 'BOOLEAN', 'Checks if a value is exactly true.', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO OperatorMetadata(operator_name, category, description, input_types, output_type) VALUES('IS_FALSE', 'BOOLEAN', 'Checks if a value is exactly false.', '["BOOLEAN"]', 'BOOLEAN');