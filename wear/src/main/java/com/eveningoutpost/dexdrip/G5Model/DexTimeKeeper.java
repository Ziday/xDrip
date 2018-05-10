package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

/**
 * Created by jamorham on 25/11/2016.
 */

public class DexTimeKeeper {
    private static final String TAG = DexTimeKeeper.class.getSimpleName();

    private static final String DEX_XMIT_START = "DEX_XMIT_START-";
    private static final long OLDEST_ALLOWED = 1512245359123L;
    private static final long DEX_TRANSMITTER_LIFE_SECONDS = 86400 * 120;

    private static String lastTransmitterId = null;

    // update the activation time stored for a transmitter
    public static void updateAge(String transmitterId, int dexTimeStamp) {

        if ((transmitterId == null) || (transmitterId.length() != 6)) {
            UserError.Log.e(TAG, "Invalid dex transmitter in updateAge: " + transmitterId);
            return;
        }
        if (dexTimeStamp < 1) {
            UserError.Log.e(TAG, "Invalid dex timestamp in updateAge: " + dexTimeStamp);
            return;
        }

        final long activation_time = JoH.tsl() - ((long) dexTimeStamp * 1000);

        if (activation_time > JoH.tsl()) {
            UserError.Log.wtf(TAG, "Transmitter activation time is in the future. Not possible to update: " + dexTimeStamp);
            return;
        }

        UserError.Log.d(TAG, "Activation time updated to: " + JoH.dateTimeText(activation_time));
        PersistentStore.setLong(DEX_XMIT_START + transmitterId, activation_time);

    }

    public static int getDexTime(String transmitterId, long timestamp) {

        if ((transmitterId == null) || (transmitterId.length() != 6)) {
            UserError.Log.e(TAG, "Invalid dex transmitter in getDexTime: " + transmitterId);
            return -3;
        }
        if (timestamp < OLDEST_ALLOWED) {
            UserError.Log.e(TAG, "Invalid timestamp in getDexTime: " + timestamp);
            return -2;
        }

        final long transmitter_start_timestamp = PersistentStore.getLong(DEX_XMIT_START + transmitterId);

        if (transmitter_start_timestamp < OLDEST_ALLOWED) {
            UserError.Log.e(TAG, "No valid timestamp stored for transmitter: " + transmitterId);
            return -1;
        }

        final long ms_since = timestamp - transmitter_start_timestamp;
        if (ms_since < 0) {
            UserError.Log.e(TAG, "Invalid timestamp comparison for transmitter id: " + transmitterId + " since: " + ms_since + "  requested ts: " + JoH.dateTimeText(timestamp) + " with tx start: " + JoH.dateTimeText(transmitter_start_timestamp));
            return -4;
        }
        lastTransmitterId = transmitterId;
        return (int) (ms_since / 1000);
    }

    public static long fromDexTimeCached(int dexTimeStamp) {
        return fromDexTime(lastTransmitterId, dexTimeStamp);
    }


    public static long fromDexTime(String transmitterId, int dexTimeStamp) {
        if ((transmitterId == null) || (transmitterId.length() != 6)) {
            UserError.Log.e(TAG, "Invalid dex transmitter in fromDexTime: " + transmitterId);
            return -3;
        }
        lastTransmitterId = transmitterId;
        final long transmitter_start_timestamp = PersistentStore.getLong(DEX_XMIT_START + transmitterId);
        if (transmitter_start_timestamp > 0) {
            return transmitter_start_timestamp + (dexTimeStamp * 1000);
        } else {
            return -1;
        }

    }

    // should we try to use this transmitter
    public static boolean isInDate(String transmitterId) {
        final int valid_time = getDexTime(transmitterId, JoH.tsl());
        return (valid_time >= 0) && (valid_time < DEX_TRANSMITTER_LIFE_SECONDS);
    }


    public static String extractForStream(String transmitterId) {
        if (transmitterId == null || transmitterId.length() == 0) return null;
        final long result = PersistentStore.getLong(DEX_XMIT_START + transmitterId);
        if (result == 0) return null;
        return transmitterId + "^" + result;
    }

    public static void injectFromStream(String stream) {
        if (stream == null) return;
        final String[] components = stream.split("\\^");
        try {
            if (components.length == 2) {
                final long time_stamp = Long.parseLong(components[1]);
                if (time_stamp > OLDEST_ALLOWED) {
                    PersistentStore.setLong(DEX_XMIT_START + components[0], time_stamp);
                    UserError.Log.d(TAG, "Updating time keeper: " + components[0] + " " + JoH.dateTimeText(time_stamp));
                } else {
                    UserError.Log.wtf(TAG, "Dex Timestamp doesn't meet criteria: " + time_stamp);
                }
            } else {
                UserError.Log.e(TAG, "Invalid injectFromStream length: " + stream);
            }
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Invalid injectFromStream: " + stream + " " + e);
        }
    }


}
