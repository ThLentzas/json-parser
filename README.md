## Json Parser

A strict JSON parser, compliant with [RFC 8259](https://datatracker.ietf.org/doc/html/rfc8259). It uses an **index
overlay approach**, where the parser constructs a buffer of indices referencing the original input data instead of
creating deep object trees. This approach minimizes memory overhead and optimizes performance.
The parser operates as an LL (look-ahead) parser, verifying the grammatical correctness of each token before advancing.

## Limits

### Input Byte Array

- **Maximum Size**: The input byte array is limited to 2,147,483,647 bytes.

### Numbers

- **Maximum Value**: The parser supports numbers within the range defined by the **IEEE 754 double-precision
  floating-point standard**:
    - **Max**: ~1.8 × 10³⁰⁸
    - **Min**: ~4.9 × 10⁻³²⁴ (absolute value)
- Numbers outside this range will result in an `OutOfRangeException`.

### Nesting Depth

- **Maximum Depth**: The parser supports a maximum nesting depth of **256** levels.

## Strict UTF-8 Validation

This parser enforces **strict UTF-8 validation** to ensure all characters in the input are properly encoded and
compliant. Unlike lenient parsers, which will replace invalid sequences with a replacement character `�`, this parser
will throw a `UTF8DecoderException`.

### Invalid UTF-8 Sequences:

1. **Overlong Encodings**: Sequences that represent a valid character using more bytes than necessary (e.g., `0xC0 0xAF`
   for code points ≤ `0x7F`).
2. **Unpaired Surrogates**: Invalid usage of surrogate code points in the range `0xD800–0xDFFF`.
3. **Out-of-Range Bytes**: Bytes outside the valid UTF-8 range (e.g., `0xF5–0xFF`).
4. **Missing Continuation Bytes**: A leading byte without the required continuation bytes.
5. **Invalid Continuation Bytes**: Continuation bytes in positions where leading bytes are expected.

```text
Input: [91, 34, -12, -65, -65, -65, 34, 93]
Output: Invalid UTF-8 byte sequence
-12 -65 -65 -65 is a 4-byte sequence with a value greater than the max value we can have for UTF-8 (U+0000..U+10FFFF) F4 BF BF BF (-12 -65 -65 -65)
```

## Node Types and Accessor Methods

The parser represents JSON data as nodes, each corresponding to a JSON type. These nodes provide **typed accessor
methods** for efficiently retrieving and processing data.

### 1. **ObjectNode**

#### Accessor Methods:

- **`keys()`**: Returns a `Set<String>` containing all the keys of the node.
- **`values()`**: Returns an `Object[]` containing all the values of the node.
- **`key(String name)`**: Returns a `Node` as the value of the key.
- **`value()`**: Returns the current node as a `Map<String, Object>`.
- **`hasKey(String name)`**: Returns true if the key exists false otherwise.
- **`path(String name)`**: Returns the child node for the specified key or a `MissingNode` if the key does not exist.
- **`parent()`**: Returns the parent node of the current node. If the node is the root, this method returns `null`.
- **`type()`**: Returns the type of the node.

### 2. **ArrayNode**

#### Accessor Methods:

- **`value()`**: Returns the array as an `Object[]`, where each element is resolved recursively by calling `value()` on
  the corresponding `Node`.
- **`get(int index)`**: Retrieves the element at the specified index as a `Node`.
- **`path(int index)`**: Returns the element at the specified index as a `Node`.
- **`parent()`**: Returns the parent node of the current node. If the node is the root, this method returns `null`.
- **`type()`**: Returns the type of the node.

### 3. **NumberNode**

#### Typed Accessor Methods:

- **`value()`**: Returns the value of the node as a `Number`.
- **`intValue()`**: Converts the number to an `int`.
- **`longValue()`**: Converts the number to a `long`.
- **`doubleValue()`**:Converts the number to a `BigDecimal`.
- **`isInteger(Number number)`**: Checks if the number is a valid integer.
- **`isLong(Number number)`**:Checks if the number is a valid long.
- **`isDouble(Number number)`**: Checks if the number is a valid double-precision floating-point number.
- **`parent()`**: Returns the parent node of the current node. If the node is the root, this method returns `null`.
- **`type()`**: Returns the type of the node.

### 4. **StringNode**

#### Accessor Methods:

- **`value()`**: Returns the plain text of the string as a `String`.
- **`type()`**: Returns the type of the node.
- **`isSubsequence(String subsequence)`**: Checks if the provided `subsequence` is a valid subsequence of the string.
- **`subsequence(List<Integer> indices)`**: Extracts a subsequence from the string based on a list of indices.
- **`intValue()`**: Converts the string to an integer if it represents a valid number.
- **`longValue()`**: Converts the string to a long if it represents a valid number.
- **`doubleValue()`**: Converts the string to a `BigDecimal` if it represents a valid floating-point number.
- **`parent()`**: Returns the parent node of the current node. If the node is the root, this method returns `null`.
- **`type()`**: Returns the type of the node.

### 5. **BooleanNode**

#### Accessor Methods:

- **`value()`**: Returns the boolean value.
- **`numericValue()`**:Returns the numeric representation of the boolean value:
- **`parent()`**: Returns the parent node of the current node. If the node is the root, this method returns `null`.
- **`type()`**: Returns the type of the node.

### 6. **NullNode**

#### Accessor Methods:

- **`value()`**: Returns `null`.
- **`parent()`**: Returns the parent node of the current node. If the node is the root, this method returns `null`.
- **`type()`**: Returns the type of the node.

### Get Started

```
byte[] bytes = {104, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100};
Decoder decoder = new Decoder();
String jsonText = decoder.decode(bytes);
Tokenizer tokenizer = new Tokenizer(jsonText);
Parser parser = new Parser(tokenizer);
Node node = parser.parse();
```
