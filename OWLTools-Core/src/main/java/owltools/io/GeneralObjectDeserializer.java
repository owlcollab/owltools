package owltools.io;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

/**
 * Use this class to create a general object-tree using the Gson
parser
 * (see http://code.google.com/p/google-gson/).  This class
deserializes
 * Gson output such that all objects are one of the following types:
 *
 * <ul>
 * <li>String
 * <li>Boolean
 * <li>Number (may be Integer, Double, BigInteger, or BigDecimal per
gson)
 * <li>Object[] (where each element is also one of these 5 types)
 * <li>Map<String, Object> (where each value is one of these 5 types)
 * </ul>
 *
 * This is often useful when creating a complete new specific
 * deserializer is too much work because just a specific subset
 * of the output is desired, or for when it isn't known exactly
 * in advance which object maps to any given json input.
 *
 * @author Brian Nettleton
 *
 */
public class GeneralObjectDeserializer implements
JsonDeserializer<Object> {

	/**
	 * <code>generalGson</code> is a Gson instance which provides
	 * for general deserializing.
	 */
	public static final Gson generalGson =
		new GsonBuilder().registerTypeAdapter(Object.class, new
				GeneralObjectDeserializer())
				.create();

	/**
	 * This is a convenience routine which deserializes a
	 * string into one of the five general Java types as
	 * described above.
	 *
	 * @param json the json string to be deserialized
	 * @return a general Java object
	 * @throws JsonParseException if json is not a valid representation
for an object of type
	 *
	 * @see com.google.gson.Gson#fromJson(String, Class)
	 */
	public static Object fromJson(String json) throws JsonParseException
	{
		return generalGson.fromJson(json, Object.class);
	}

	/**
	 * This is a convenience routine deserializes the Json read from the
	 * specified reader into one of the five general Java types as
	 * described above.
	 *
	 * @param json the reader producing the Json from which the object is
to be deserialized.
	 * @return a general Java object
	 * @throws JsonParseException if json is not a valid representation
for an object of type
	 *
	 * @see com.google.gson.Gson#fromJson(Reader , Class)
	 */
	public static Object fromJson(Reader json) throws JsonParseException
	{
		return generalGson.fromJson(json, Object.class);
	}

	/* (non-Javadoc)
	 * @see
com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement,
java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
	 */
	public Object deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		if( json.isJsonNull() ) {
			return null;
		}
		else if ( json.isJsonPrimitive() ) {
			JsonPrimitive primitive = json.getAsJsonPrimitive();
			if ( primitive.isString() ) {
				return primitive.getAsString();
			}
			else if ( primitive.isNumber() ) {
				return primitive.getAsNumber();
			}
			else if ( primitive.isBoolean() ) {
				return primitive.getAsBoolean();
			}
		}
		else if ( json.isJsonArray() ) {
			JsonArray array = json.getAsJsonArray();
			Object[] result = new Object[array.size()];
			int i = 0;
			for( JsonElement element : array ) {
				result[i] = deserialize(element, null, context);
				++i;
			}
			return result;
		}
		else if ( json.isJsonObject() ) {
			JsonObject object = json.getAsJsonObject();
			Map<String, Object> result = new HashMap<String,Object>();
			for( Map.Entry<String, JsonElement> entry : object.entrySet() ) {
				Object value = deserialize(entry.getValue(), null, context);
				result.put(entry.getKey(), value);
			}
			return result;
		}
		else {
			throw new JsonParseException("Unknown JSON type for JsonElement " +
					json.toString());
		}
		return null;
	} 
}