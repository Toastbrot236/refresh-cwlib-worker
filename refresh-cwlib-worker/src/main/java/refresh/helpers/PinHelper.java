package refresh.helpers;

import cwlib.resources.RTranslationTable;

// Generates progress types, pin IDs, name LAMS, and description LAMS based on group ID (and target value)
public abstract class PinHelper {
    public static long getProgressType(String customGroupId) {
        return RTranslationTable.makeLamsKeyID(customGroupId + "_PROGRESS_TYPE");
    }

    public static long getPinId(String customGroupId, int targetValue) {
        return RTranslationTable.makeLamsKeyID(customGroupId + "_VALUE_" + targetValue + "_PIN_ID");
    }

    public static long getNameLams(String customGroupId, int targetValue) {
        return RTranslationTable.makeLamsKeyID(customGroupId + "_VALUE_" + targetValue + "_NAME");
    }

    public static long getDescriptionLams(String customGroupId, int targetValue) {
        return RTranslationTable.makeLamsKeyID(customGroupId + "_VALUE_" + targetValue + "_DESC");
    }
}
