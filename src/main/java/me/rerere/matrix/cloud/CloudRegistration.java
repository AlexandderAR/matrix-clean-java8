package me.rerere.matrix.cloud;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

/** Helper emitted by registration generation to apply the selected Cloud action. */
public final class CloudRegistration {
    public interface Actions {
        void startInference();
        void requestCloudMetadata();
    }

    private CloudRegistration() { }

    public static boolean applySelected(List<String> selected, Actions actions) {
        boolean found = false;
        if (!selected.isEmpty()) {
            for (String item : selected) {
                if (StringsKt.contains(item, "Cloud", false)) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            actions.startInference();
            actions.requestCloudMetadata();
        }
        return found;
    }
}
