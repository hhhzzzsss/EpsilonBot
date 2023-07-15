package com.github.hhhzzzsss.epsilonbot.buildsync;

import com.google.gson.*;

import java.lang.reflect.Type;

public class PlotCoord {
    public final int x;
    public final int z;

    public PlotCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public PlotCoord(String str) {
        String[] split = str.substring(1, str.length()-1).split(",");
        this.x = Integer.parseInt(split[0]);
        this.z = Integer.parseInt(split[1]);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlotCoord plotCoord = (PlotCoord) o;
        return x == plotCoord.x && z == plotCoord.z;
    }

    @Override
    public int hashCode() {
        return 31*x + z + 17;
    }

    @Override
    public String toString() {
        return "(" + x + "," + z + ")";
    }

	public static class PlotCoordDeserializer implements JsonDeserializer<PlotCoord> {
        @Override
        public PlotCoord deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                return new PlotCoord(jsonObject.get("x").getAsInt(), jsonObject.get("z").getAsInt());
            } else {
                String str = jsonElement.getAsString();
                return new PlotCoord(str);
            }
        }
    }
}
