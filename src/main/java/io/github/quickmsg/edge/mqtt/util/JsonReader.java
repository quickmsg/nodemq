package io.github.quickmsg.edge.mqtt.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author luxurong
 */
public class JsonReader {
    private static final ObjectMapper mapper = new ObjectMapper();
//
//    static {
//        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
//        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
//        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
//        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
//        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//    }


    public static <T> T readJson(String path, Class<T> tClass) {
        try {
            return mapper.readValue(new File(path), tClass);
        } catch (IOException e) {
            //ignore
            return null;
        }
    }


    public static <T> List<T> readJsonList(String path) {
        try {
            return mapper.readValue(new File(path), new TypeReference<List<T>>() {
            });
        } catch (IOException e) {
            //ignore
            return null;
        }
    }


    public static String bean2Json(Object data) {
        try {
            if (data instanceof String s) {
                return s;
            } else {

                return mapper.writeValueAsString(data);
            }
        } catch (JsonProcessingException e) {
            return "";
        }
    }


    public static Map<String, Object> bean2Map(Object data) {
        try {
            return mapper.convertValue(data, new TypeReference<Map<String, Object>>() {
                @Override
                public Type getType() {
                    return super.getType();
                }
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }

    }


    public static <T> T json2Bean(String jsonData, Class<T> beanType) {
        try {
            return mapper.readValue(jsonData, beanType);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T json2Bean(String jsonData, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(jsonData, typeReference);
        } catch (Exception e) {
            return null;
        }
    }


    public static <T> T object2Bean(Object jsonData, Class<T> beanType) {
        try {
            return mapper.convertValue(jsonData, beanType);
        } catch (Exception e) {
            return null;
        }
    }


    public static <T> List<T> json2List(String jsonData, Class<T> beanType) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
            return mapper.readValue(jsonData, javaType);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static <K, V> Map<K, V> json2Map(String jsonData, Class<K> keyType, Class<V> valueType) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, keyType, valueType);

            return mapper.readValue(jsonData, javaType);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static <K, V> String map2Json(Map<K, V> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "";
        }
    }


    public static Object dynamic(String s) {
        if (s.startsWith("{") && s.endsWith("}")) {
            return JsonReader.json2Map(s, String.class, Object.class);
        } else if (s.startsWith("[") && s.endsWith("]")) {
            return json2List(s, Map.class);
        } else {
            return s;
        }
    }

    public static boolean isValidJson(byte[] byteArray) {
        try {
            JsonNode jsonNode = mapper.readTree(byteArray);
            return jsonNode.isObject() || jsonNode.isArray();
        } catch (Exception e) {
            return false;
        }
    }

    public static String dynamicJson(Object object) {
        if (object instanceof String) {
            return (String) object;
        } else {
            return JsonReader.bean2Json(object);
        }
    }


}
