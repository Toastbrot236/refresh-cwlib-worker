package refresh.common.helpers;

import cwlib.types.data.ResourceDescriptor;

public abstract class ResourceHelper {
    public static String asString(ResourceDescriptor descriptor) {
        if (descriptor.isGUID()) {
            return descriptor.getGUID().toString();
        }
        else if (descriptor.isHash()) {
            return descriptor.getSHA1().toString();
        }
        else {
            return "0";
        }
    }
}
