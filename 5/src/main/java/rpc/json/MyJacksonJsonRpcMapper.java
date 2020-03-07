package rpc.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.rabbitmq.tools.jsonrpc.JacksonJsonRpcMapper;
import com.rabbitmq.tools.jsonrpc.JsonRpcMappingException;
import com.rabbitmq.tools.jsonrpc.ProcedureDescription;
import com.rabbitmq.tools.jsonrpc.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MyJacksonJsonRpcMapper extends JacksonJsonRpcMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyJacksonJsonRpcMapper.class);

    @Override
    public JsonRpcRequest parse(String requestBody, ServiceDescription description) {
        JsonFactory jsonFactory = new MappingJsonFactory();
        String method = null, version = null;
        final List<TreeNode> parameters = new ArrayList<>();
        Object id = null;
        try (JsonParser parser = jsonFactory.createParser(requestBody)) {
            while (parser.nextToken() != null) {
                JsonToken token = parser.currentToken();
                if (token == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    token = parser.nextToken();
                    if ("method".equals(name)) {
                        method = parser.getValueAsString();
                    } else if ("id".equals(name)) {
                        TreeNode node = parser.readValueAsTree();
                        if (node instanceof ValueNode) {
                            ValueNode idNode = (ValueNode) node;
                            if (idNode.isNull()) {
                                id = null;
                            } else if (idNode.isTextual()) {
                                id = idNode.asText();
                            } else if (idNode.isNumber()) {
                                id = idNode.asLong();
                            } else {
                                LOGGER.warn("ID type not null, text, or number {}, ignoring", idNode);
                            }
                        } else {
                            LOGGER.warn("ID not a scalar value {}, ignoring", node);
                        }
                    } else if ("version".equals(name)) {
                        version = parser.getValueAsString();
                    } else if ("params".equals(name)) {
                        if (token == JsonToken.START_ARRAY) {
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                parameters.add(parser.readValueAsTree());
                            }
                        } else {
                            throw new IllegalStateException("Field params must be an array");
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new JsonRpcMappingException("Error during JSON parsing", e);
        }

        if (method == null) {
            throw new IllegalArgumentException("Could not find method to invoke in request");
        }

        List<Object> convertedParameters = new ArrayList<>(parameters.size() - 1);
        if (parameters.size() - 1 != 0) {
            ProcedureDescription proc = description.getProcedure(method, parameters.size() - 1);
            Method internalMethod = proc.internal_getMethod();
            for (int i = 0; i < internalMethod.getParameterCount(); i++) {
                TreeNode parameterNode = parameters.get(i);
                try {
                    Class<?> parameterType = internalMethod.getParameterTypes()[i];
                    Object value = convert(parameterNode, parameterType);
                    convertedParameters.add(value);
                } catch (IOException e) {
                    throw new JsonRpcMappingException("Error during parameter conversion", e);
                }
            }
        }
        convertedParameters.add(parameters.get(parameters.size() - 1));

        return new JsonRpcRequest(
                id, version, method,
                convertedParameters.toArray()
        );
    }
}
