-- General Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('EXISTS', 'Checks if one or more JSON paths exist.', '["PROVIDER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_EXISTS', 'Checks if one or more JSON paths do not exist.', '["PROVIDER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('IS_NULL', 'Checks if a value is explicitly null.', '["ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('IS_NOT_NULL', 'Checks if a value exists and is not null.', '["ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('TYPE_IS', 'Checks the Java type of a value.', '["STRING", "ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('EQUALS', 'Checks if two or more values are equal.', '["ANY", "ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_EQUALS', 'Checks if two or more values are not equal.', '["ANY", "ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('IN_SET', 'Checks if a value is present in a set of values.', '["ANY", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_IN_SET', 'Checks if a value is not present in a set of values.', '["ANY", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('ONE_OF', 'Succeeds if at least one input is true (predicative).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('ALL_OF', 'Succeeds if all inputs are true (predicative).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NONE_OF', 'Succeeds if no input is true (predicative).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('AND', 'Performs a strict logical AND operation.', '["BOOLEAN", "BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('OR', 'Performs a strict logical OR operation.', '["BOOLEAN", "BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('XOR', 'Checks if exactly one input is true (strict).', '["BOOLEAN", "BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT', 'Negates a single boolean value (strict).', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('MATCHES_SCHEMA', 'Validates a JSON document against a JSON schema.', '["JSON", "SCHEMA"]', 'BOOLEAN');

-- Number Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('GREATER_THAN', 'Checks if a number is greater than another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LESS_THAN', 'Checks if a number is less than another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('GREATER_OR_EQUAL', 'Checks if a number is greater than or equal to another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LESS_OR_EQUAL', 'Checks if a number is less than or equal to another.', '["NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('BETWEEN', 'Checks if a number is between two bounds (inclusive).', '["NUMBER", "NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_BETWEEN', 'Checks if a number is outside of two bounds.', '["NUMBER", "NUMBER", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('ADD', 'Adds two or more numbers.', '["NUMBER", "NUMBER"]', 'NUMBER');

-- Array/List Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LENGTH_EQUALS', 'Compares the length of a list with a number.', '["ARRAY", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LENGTH_GT', 'Checks if the length of a list is greater than a number.', '["ARRAY", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LENGTH_LT', 'Checks if the length of a list is less than a number.', '["ARRAY", "NUMBER"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_EMPTY', 'Checks if a list or array is not empty.', '["ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('CONTAINS_ELEMENT', 'Checks if an element is present in a list.', '["ARRAY", "ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_CONTAINS_ELEMENT', 'Checks if an element is not present in a list.', '["ARRAY", "ANY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('CONTAINS_ALL', 'Checks if a list contains all elements from another list.', '["ARRAY", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('CONTAINS_ANY', 'Checks if a list contains at least one element from another list.', '["ARRAY", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('CONTAINS_NONE', 'Checks if a list contains no elements from another list.', '["ARRAY", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('SUM', 'Calculates the sum of all numbers in a list.', '["ARRAY"]', 'NUMBER');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('AVG', 'Calculates the average of all numbers in a list.', '["ARRAY"]', 'NUMBER');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('MIN', 'Finds the smallest number in a list.', '["ARRAY"]', 'NUMBER');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('MAX', 'Finds the largest number in a list.', '["ARRAY"]', 'NUMBER');

-- Object Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('HAS_KEY', 'Checks if a JSON object has a specific key.', '["JSON", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LACKS_KEY', 'Checks if a JSON object lacks a specific key.', '["JSON", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('HAS_ALL_KEYS', 'Checks if a JSON object has all specified keys.', '["JSON", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('HAS_ANY_KEY', 'Checks if a JSON object has at least one of the specified keys.', '["JSON", "ARRAY"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('HAS_NO_KEYS', 'Checks if a JSON object has none of the specified keys.', '["JSON", "ARRAY"]', 'BOOLEAN');

-- String Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('EQUALS_CASE_SENSITIVE', 'Performs a case-sensitive comparison of two strings.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('EQUALS_IGNORE_CASE', 'Performs a case-insensitive comparison of two strings.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('CONTAINS', 'Checks if a string contains another string.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('NOT_CONTAINS', 'Checks if a string does not contain another string.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('STARTS_WITH', 'Checks if a string starts with a specific prefix.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('ENDS_WITH', 'Checks if a string ends with a specific suffix.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('REGEX_MATCH', 'Checks if a string matches a regular expression pattern.', '["STRING", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('LENGTH_BETWEEN', 'Checks if a string''s length is between two numbers.', '["STRING", "NUMBER", "NUMBER"]', 'BOOLEAN');

-- Date/Time Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('BEFORE', 'Checks if a date is before another date.', '["DATE", "DATE"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('AFTER', 'Checks if a date is after another date.', '["DATE", "DATE"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('BETWEEN_DATES', 'Checks if a date is between two other dates.', '["DATE", "DATE", "DATE"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('SAME_DAY', 'Checks if two dates are on the same calendar day.', '["DATE", "DATE"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('AGE_GREATER_THAN', 'Checks if a date is older than a specific duration.', '["DATE", "NUMBER", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('WITHIN_LAST', 'Checks if a date is within the last specified duration.', '["DATE", "NUMBER", "STRING"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('WITHIN_NEXT', 'Checks if a date is within the next specified duration.', '["DATE", "NUMBER", "STRING"]', 'BOOLEAN');

-- Boolean Predicates
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('IS_TRUE', 'Checks if a value is exactly true.', '["BOOLEAN"]', 'BOOLEAN');
INSERT INTO operator_metadata(operator_name, description, input_types, output_type) VALUES('IS_FALSE', 'Checks if a value is exactly false.', '["BOOLEAN"]', 'BOOLEAN');