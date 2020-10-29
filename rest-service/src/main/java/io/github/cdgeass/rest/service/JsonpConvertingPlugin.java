package io.github.cdgeass.rest.service;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsMediaType;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

import javax.json.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.osgi.util.converter.ConverterFunction.CANNOT_HANDLE;

/**
 * @author cdgeass
 * @since  2020-10-29
 */
@Component(scope = ServiceScope.PROTOTYPE)
@JaxrsExtension
@JaxrsMediaType(APPLICATION_JSON)
public class JsonpConvertingPlugin<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private final Converter converter = Converters.newConverterBuilder()
            .rule(JsonValue.class, this::toJsonValue)
            .rule(this::toScalar)
            .build();

    private JsonValue toJsonValue(Object value, Type targetType) {
        if (value == null) {
            return JsonValue.NULL;
        } else if (value instanceof String) {
            return Json.createValue(value.toString());
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (value instanceof Number) {
            Number n = (Number) value;
            if (value instanceof Float || value instanceof Double) {
                return Json.createValue(n.doubleValue());
            } else if (value instanceof BigDecimal) {
                return Json.createValue((BigDecimal) value);
            } else if (value instanceof BigInteger) {
                return Json.createValue((BigInteger) value);
            } else {
                return Json.createValue(n.longValue());
            }
        } else if (value instanceof Collection || value.getClass().isArray()) {
            return toJsonArray(value);
        } else {
            return toJsonObject(value);
        }
    }

    private JsonArray toJsonArray(Object o) {
        List<?> l = converter.convert(o).to(List.class);

        JsonArrayBuilder builder = Json.createArrayBuilder();
        l.forEach(v -> builder.add(toJsonValue(v, JsonValue.class)));
        return builder.build();
    }

    private JsonObject toJsonObject(Object o) {

        Map<String, Object> m = converter.convert(o).to(new TypeReference<Map<String, Object>>(){});

        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        m.forEach((key, value) -> jsonBuilder.add(key, toJsonValue(value, JsonValue.class)));
        return jsonBuilder.build();
    }

    private Object toScalar(Object o, Type t) {

        if (o instanceof JsonNumber) {
            JsonNumber jn = (JsonNumber) o;
            return converter.convert(jn.bigDecimalValue()).to(t);
        } else if (o instanceof JsonString) {
            JsonString js = (JsonString) o;
            return converter.convert(js.getString()).to(t);
        } else if (o instanceof JsonValue) {
            JsonValue jv = (JsonValue) o;
            if (jv.getValueType() == JsonValue.ValueType.NULL) {
                return null;
            } else if (jv.getValueType() == JsonValue.ValueType.TRUE) {
                return converter.convert(Boolean.TRUE).to(t);
            } else if (jv.getValueType() == JsonValue.ValueType.FALSE) {
                return converter.convert(Boolean.FALSE).to(t);
            }
        }
        return CANNOT_HANDLE;
    }

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return APPLICATION_JSON_TYPE.isCompatible(mediaType) || mediaType.getSubtype().endsWith("+json");
    }

    @Override
    public T readFrom(Class<T> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        try (JsonReader jr = Json.createReader(inputStream)) {
            JsonStructure read = jr.read();
            return (T) converter.convert(read).to(type);
        }
    }

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return APPLICATION_JSON_TYPE.isCompatible(mediaType) || mediaType.getSubtype().endsWith("+json");
    }

    @Override
    public void writeTo(T t, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
        JsonValue jv = converter.convert(t).to(JsonValue.class);

        try (JsonWriter jw = Json.createWriter(outputStream)) {
            jw.write(jv);
        }
    }
}
