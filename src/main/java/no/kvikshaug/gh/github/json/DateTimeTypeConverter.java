package no.kvikshaug.gh.github.json;

import com.google.gson.*;
import org.joda.time.DateTime;

import java.lang.reflect.Type;
import java.text.*;
import java.util.Date;

// nicked from: http://sites.google.com/site/gson/gson-type-adapters-for-common-classes-1
public class DateTimeTypeConverter
        implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
    public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            try {
                return new DateTime(json.getAsString());
            } catch (IllegalArgumentException e) {
                // May be it came in formatted as a java.util.Date, so try that
                Date date = context.deserialize(json, Date.class);
                return new DateTime(date);
            }
        } catch (JsonParseException jpe) {
            // Github seems to use a format that doesn't work with java.util.Date
            SimpleDateFormat githubFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            try {
                return new DateTime(githubFormat.parse(json.getAsString()));
            } catch (ParseException eFirst) {
                // Github seems to use ANOTHER format that doesn't work with java.util.Date (what the hell?)
                SimpleDateFormat secondGithubFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
                try {
                    return new DateTime(secondGithubFormat.parse(json.getAsString()));
                } catch (ParseException eSecond) {
                    System.err.println("GithubPostReceive: Unable to parse date!");
                    eSecond.printStackTrace(System.err);
                }
            }
            return null;
        }
    }
}
