import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.*;


public class MyJson {

    private static Logger logger = LoggerFactory.getLogger(MyJson.class);

    public String toJson(Object o) {
        try {
            if (isNull(o)) {
                return JsonValue.NULL.toString();
            }
            Class clazz = o.getClass();
            if (clazz.isArray()) {
                return arrayToJson(o).toString();
            } else if (Collection.class.isAssignableFrom(clazz)) {
                if(((Collection)o).size() == 1) {
                    return arrayToJson(((Collection) o).toArray()).toString();
                }
            } else {
                JsonObject jsonObject = objectToJson(o);
                return writeToString(jsonObject);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JsonObject objectToJson(Object object) throws IllegalAccessException {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        Field[] fields = object.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            int mod = field.getModifiers();

            if (!(Modifier.isStatic(mod) || Modifier.isTransient(mod))) {
                objectBuilder.add(field.getName(), elementToJson(field.get(object), field.getType()));
            }
        }
        return objectBuilder.build();
    }

    private JsonValue elementToJson(Object element, Class<?> type) {
        if (!nonNull(element)) {
            return JsonValue.NULL;
        } else if (type.isPrimitive()) {
            return simpleObjectToJson(element);
        } else if (type.isArray()) {
            return arrayToJson(element);
        } else if (Collection.class.isAssignableFrom(type)) {
            Collection<?> collection = (Collection<?>) element;
            return arrayToJson(collection.toArray());
        } else if (Map.class.isAssignableFrom(type)) {
            return mapToJson(element);
        } else {
            return simpleObjectToJson(element);
        }
    }

    private JsonValue mapToJson(Object object) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        Map<?, ?> map = (Map<?, ?>) object;

        for (Object key : map.keySet()) {
            Object value = map.get(key);
            objectBuilder.add(key.toString(), elementToJson(value, value.getClass()));
        }
        return objectBuilder.build();
    }

    private JsonValue arrayToJson(Object object) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        int length = Array.getLength(object);

        for (int i = 0; i < length; i++) {
            Object temp = Array.get(object, i);
            arrayBuilder.add(elementToJson(temp, temp.getClass()));
        }
        return arrayBuilder.build();
    }

    private JsonValue simpleObjectToJson(Object object) {
        if (object == null) {
            return JsonValue.NULL;
        } else if (object instanceof Number) {
            Number number = (Number) object;
            if (number instanceof Float || number instanceof Double) {
                return Json.createValue(number.doubleValue());
            } else if (number instanceof Long) {
                return Json.createValue(number.longValue());
            } else {
                return Json.createValue(number.intValue());
            }
        } else if (object instanceof Boolean) {
            if (object.equals(true)) {
                return JsonValue.TRUE;
            } else {
                return JsonValue.FALSE;
            }
        } else {
            return Json.createValue(object.toString());
        }
    }

    private static String writeToString(JsonObject jsonst) {
        StringWriter stWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stWriter)) {
            jsonWriter.writeObject(jsonst);
        }

        return stWriter.toString();
    }
}
