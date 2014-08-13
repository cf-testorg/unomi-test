package org.oasis_open.wemi.context.server.impl.services;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.oasis_open.wemi.context.server.api.conditions.*;
import org.oasis_open.wemi.context.server.api.consequences.Consequence;
import org.oasis_open.wemi.context.server.api.consequences.ConsequenceType;
import org.oasis_open.wemi.context.server.api.services.DefinitionsService;
import org.oasis_open.wemi.context.server.persistence.spi.PropertyTypedObjectDeserializer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;

public class ParserHelper {

    public static Condition parseCondition(DefinitionsService service, JsonObject object) {
        ConditionType typeNode = service.getConditionType(object.getString("type"));
        JsonObject parameterValues = object.getJsonObject("parameterValues");

        Condition node = new Condition();
        node.setConditionType(typeNode);
        Map<String, Object> values = new HashMap<String, Object>();
        node.setParameterValues(values);

        for (Parameter parameter : typeNode.getParameters()) {
            Object parameterValue = null;
            if (parameter.isMultivalued()) {
                final ArrayList<Object> objects = new ArrayList<Object>();
                JsonArray array = parameterValues.getJsonArray(parameter.getId());
                for (JsonValue value : array) {
                    objects.add(getParameterValue(service, parameter, value));
                }
                parameterValue = objects;
            } else {
                parameterValue = getParameterValue(service, parameter, parameterValues.get(parameter.getId()));
            }
            values.put(parameter.getId(), parameterValue);
        }
        return node;
    }

    public static Consequence parseConsequence(DefinitionsService service, JsonObject object) {
        ConsequenceType typeNode = service.getConsequenceType(object.getString("type"));
        JsonObject parameterValues = object.getJsonObject("parameterValues");

        Consequence node = new Consequence();
        node.setConsequenceType(typeNode);
        Map<String, Object> values = new HashMap<String, Object>();
        node.setParameterValues(values);

        for (Parameter parameter : typeNode.getParameters()) {
            Object parameterValue = null;
            if (parameter.isMultivalued()) {
                final ArrayList<Object> objects = new ArrayList<Object>();
                JsonArray array = parameterValues.getJsonArray(parameter.getId());
                for (JsonValue value : array) {
                    objects.add(getParameterValue(service, parameter, value));
                }
                parameterValue = objects;
            } else {
                parameterValue = getParameterValue(service, parameter, parameterValues.get(parameter.getId()));
            }
            values.put(parameter.getId(), parameterValue);
        }
        return node;
    }

    private static Object getParameterValue(DefinitionsService service, Parameter parameter, JsonValue value) {
        if (parameter.getType().equals("Condition")) {
            return parseCondition(service, (JsonObject) value);
        } else if (parameter.getType().equals("comparisonOperator")) {
            return ((JsonString)value).getString();
        } else if (parameter.getType().equals("string")) {
            return ((JsonString)value).getString();
        }
        return null;
    }


    public static List<Parameter> parseParameters(JsonObject conditionObject) {
        List<Parameter> parameters = new ArrayList<Parameter>();
        JsonArray parameterArray = conditionObject.getJsonArray("parameters");
        for (int i = 0; i < parameterArray.size(); i++) {
            JsonObject parameterObject = parameterArray.getJsonObject(i);
            String paramId = parameterObject.getString("id");
            String paramName = parameterObject.getString("name");
            String paramDescription = parameterObject.getString("description");
            String paramType = parameterObject.getString("type");
            boolean multivalued = parameterObject.getBoolean("multivalued");
            String paramChoiceListInitializerFilter = null;
            try {
                paramChoiceListInitializerFilter = parameterObject.getString("choicelistInitializerFilter");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Parameter conditionParameter = new Parameter(paramId, paramName, paramDescription, paramType, multivalued, paramChoiceListInitializerFilter);
            parameters.add(conditionParameter);
        }
        return parameters;
    }

    public static void resolveConditionTypes(DefinitionsService definitionsService, Condition rootCondition) {
        if (rootCondition.getConditionType() == null) {
            ConditionType conditionType = definitionsService.getConditionType(rootCondition.getConditionTypeId());
            if (conditionType != null) {
                rootCondition.setConditionType(conditionType);
            }
        }
        // recursive call for sub-conditions as parameters
        for (Object parameterValue : rootCondition.getParameterValues().values()) {
            if (parameterValue instanceof Condition) {
                resolveConditionTypes(definitionsService, (Condition) parameterValue);
            } else if (parameterValue instanceof Collection) {
                Collection<Object> valueList = (Collection<Object>) parameterValue;
                for (Object value : valueList) {
                    if (value instanceof Condition) {
                        resolveConditionTypes(definitionsService, (Condition) value);
                    }
                }
            }
        }
    }
}
