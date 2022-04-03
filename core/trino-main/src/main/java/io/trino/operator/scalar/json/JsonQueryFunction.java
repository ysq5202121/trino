/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.operator.scalar.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import io.trino.annotation.UsedByGeneratedCode;
import io.trino.json.ir.IrJsonPath;
import io.trino.json.ir.TypedValue;
import io.trino.metadata.BoundSignature;
import io.trino.metadata.FunctionManager;
import io.trino.metadata.FunctionMetadata;
import io.trino.metadata.Metadata;
import io.trino.metadata.Signature;
import io.trino.metadata.SqlScalarFunction;
import io.trino.operator.scalar.ChoicesScalarFunctionImplementation;
import io.trino.operator.scalar.ScalarFunctionImplementation;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeManager;
import io.trino.spi.type.TypeSignature;
import io.trino.sql.planner.JsonPathEvaluator;
import io.trino.sql.planner.JsonPathEvaluator.PathEvaluationError;
import io.trino.sql.tree.JsonQuery.ArrayWrapperBehavior;
import io.trino.sql.tree.JsonQuery.EmptyOrErrorBehavior;
import io.trino.type.JsonPath2016Type;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.trino.json.JsonInputErrorNode.JSON_ERROR;
import static io.trino.json.ir.SqlJsonLiteralConverter.getJsonNode;
import static io.trino.operator.scalar.json.ParameterUtil.getParametersArray;
import static io.trino.spi.function.InvocationConvention.InvocationArgumentConvention.BOXED_NULLABLE;
import static io.trino.spi.function.InvocationConvention.InvocationArgumentConvention.NEVER_NULL;
import static io.trino.spi.function.InvocationConvention.InvocationReturnConvention.NULLABLE_RETURN;
import static io.trino.spi.type.StandardTypes.JSON_2016;
import static io.trino.spi.type.StandardTypes.TINYINT;
import static io.trino.util.Reflection.methodHandle;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class JsonQueryFunction
        extends SqlScalarFunction
{
    public static final String JSON_QUERY_FUNCTION_NAME = "$json_query";
    private static final MethodHandle METHOD_HANDLE = methodHandle(JsonQueryFunction.class, "jsonQuery", FunctionManager.class, Metadata.class, TypeManager.class, Type.class, ConnectorSession.class, JsonNode.class, IrJsonPath.class, Block.class, long.class, long.class, long.class);
    private static final JsonNode EMPTY_ARRAY_RESULT = new ArrayNode(JsonNodeFactory.instance);
    private static final JsonNode EMPTY_OBJECT_RESULT = new ObjectNode(JsonNodeFactory.instance);
    private static final TrinoException INPUT_ARGUMENT_ERROR = new JsonInputConversionError("malformed input argument to JSON_QUERY function");
    private static final TrinoException PATH_PARAMETER_ERROR = new JsonInputConversionError("malformed JSON path parameter to JSON_QUERY function");
    private static final TrinoException NO_ITEMS = new JsonOutputConversionError("JSON path found no items");
    private static final TrinoException MULTIPLE_ITEMS = new JsonOutputConversionError("JSON path found multiple items");

    private final FunctionManager functionManager;
    private final Metadata metadata;
    private final TypeManager typeManager;

    public JsonQueryFunction(FunctionManager functionManager, Metadata metadata, TypeManager typeManager)
    {
        super(FunctionMetadata.scalarBuilder()
                .signature(Signature.builder()
                        .name(JSON_QUERY_FUNCTION_NAME)
                        .typeVariable("T")
                        .returnType(new TypeSignature(JSON_2016))
                        .argumentTypes(ImmutableList.of(
                                new TypeSignature(JSON_2016),
                                new TypeSignature(JsonPath2016Type.NAME),
                                new TypeSignature("T"),
                                new TypeSignature(TINYINT),
                                new TypeSignature(TINYINT),
                                new TypeSignature(TINYINT)))
                        .build())
                .nullable()
                .argumentNullability(false, false, true, false, false, false)
                .hidden()
                .description("Extracts a JSON value from a JSON value")
                .build());

        this.functionManager = requireNonNull(functionManager, "functionManager is null");
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
    }

    @Override
    protected ScalarFunctionImplementation specialize(BoundSignature boundSignature)
    {
        Type parametersRowType = boundSignature.getArgumentType(2);
        MethodHandle methodHandle = METHOD_HANDLE
                .bindTo(functionManager)
                .bindTo(metadata)
                .bindTo(typeManager)
                .bindTo(parametersRowType);
        return new ChoicesScalarFunctionImplementation(
                boundSignature,
                NULLABLE_RETURN,
                ImmutableList.of(BOXED_NULLABLE, BOXED_NULLABLE, BOXED_NULLABLE, NEVER_NULL, NEVER_NULL, NEVER_NULL),
                methodHandle);
    }

    @UsedByGeneratedCode
    public static JsonNode jsonQuery(
            FunctionManager functionManager,
            Metadata metadata,
            TypeManager typeManager,
            Type parametersRowType,
            ConnectorSession session,
            JsonNode inputExpression,
            IrJsonPath jsonPath,
            Block parametersRow,
            long wrapperBehavior,
            long emptyBehavior,
            long errorBehavior)
    {
        if (inputExpression.equals(JSON_ERROR)) {
            return handleSpecialCase(errorBehavior, INPUT_ARGUMENT_ERROR); // ERROR ON ERROR was already handled by the input function
        }
        Object[] parameters = getParametersArray(parametersRowType, parametersRow);
        for (Object parameter : parameters) {
            if (parameter.equals(JSON_ERROR)) {
                return handleSpecialCase(errorBehavior, PATH_PARAMETER_ERROR); // ERROR ON ERROR was already handled by the input function
            }
        }
        JsonPathEvaluator pathEvaluator = new JsonPathEvaluator(inputExpression, parameters, functionManager, metadata, typeManager, session);
        List<Object> pathResult;
        try {
            pathResult = pathEvaluator.evaluate(jsonPath);
        }
        catch (PathEvaluationError e) {
            return handleSpecialCase(errorBehavior, e);
        }

        // handle empty sequence
        if (pathResult.isEmpty()) {
            return handleSpecialCase(emptyBehavior, NO_ITEMS);
        }

        // translate sequence to JSON items
        List<JsonNode> sequence = pathResult.stream()
                .map(item -> {
                    if (item instanceof TypedValue) {
                        Optional<JsonNode> jsonNode = getJsonNode((TypedValue) item);
                        if (jsonNode.isEmpty()) {
                            return handleSpecialCase(errorBehavior, new JsonOutputConversionError(format(
                                    "JSON path returned a scalar SQL value of type %s that cannot be represented as JSON",
                                    ((TypedValue) item).getType())));
                        }
                        return jsonNode.get();
                    }
                    return (JsonNode) item;
                })
                .collect(toImmutableList());

        // apply array wrapper behavior
        switch (ArrayWrapperBehavior.values()[(int) wrapperBehavior]) {
            case WITHOUT:
                // do nothing
                break;
            case UNCONDITIONAL:
                sequence = ImmutableList.of(new ArrayNode(JsonNodeFactory.instance, sequence));
                break;
            case CONDITIONAL:
                if (sequence.size() != 1 || (!sequence.get(0).isArray() && !sequence.get(0).isObject())) {
                    sequence = ImmutableList.of(new ArrayNode(JsonNodeFactory.instance, sequence));
                }
                break;
            default:
                throw new IllegalStateException("unexpected array wrapper behavior");
        }

        // singleton sequence - return the only item
        if (sequence.size() == 1) {
            return sequence.get(0);
            // if the only item is a TextNode, need to apply the KEEP / OMIT QUOTES behavior. this is done by the JSON output function
        }

        return handleSpecialCase(errorBehavior, MULTIPLE_ITEMS);
    }

    private static JsonNode handleSpecialCase(long behavior, TrinoException error)
    {
        switch (EmptyOrErrorBehavior.values()[(int) behavior]) {
            case NULL:
                return null;
            case ERROR:
                throw error;
            case EMPTY_ARRAY:
                return EMPTY_ARRAY_RESULT;
            case EMPTY_OBJECT:
                return EMPTY_OBJECT_RESULT;
        }
        throw new IllegalStateException("unexpected behavior");
    }
}