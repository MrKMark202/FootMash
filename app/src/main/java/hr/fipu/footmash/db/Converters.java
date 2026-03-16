package hr.fipu.footmash.db;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Room TypeConverter za pretvaranje List<Integer> u JSON string i obrnuto.
 */
public class Converters {

    @TypeConverter
    public static String fromIntegerList(List<Integer> list) {
        if (list == null) return null;
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static List<Integer> toIntegerList(String json) {
        if (json == null) return null;
        Type type = new TypeToken<List<Integer>>() {}.getType();
        return new Gson().fromJson(json, type);
    }
}
