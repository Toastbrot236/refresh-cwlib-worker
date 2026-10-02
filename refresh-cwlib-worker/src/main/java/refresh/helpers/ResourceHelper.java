package refresh.helpers;

import cwlib.types.data.ResourceDescriptor;

public abstract class ResourceHelper {
    public static String GetAssetReference(ResourceDescriptor descriptor) {
        if (descriptor == null) return "0";
        else if (descriptor.isHash()) return descriptor.getSHA1().toString();
        else if (descriptor.isGUID()) return descriptor.getGUID().toString();
        else return "0";
    }
}
