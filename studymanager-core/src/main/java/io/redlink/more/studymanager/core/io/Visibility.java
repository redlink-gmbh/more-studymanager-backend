package io.redlink.more.studymanager.core.io;

public class Visibility {

    public static final Visibility DEFAULT = new Visibility(true, false);
    boolean changeable;
    boolean hiddenByDefault;

    public Visibility() {
    }

    public Visibility(boolean changeable, boolean hiddenByDefault) {
        this.changeable = changeable;
        this.hiddenByDefault = hiddenByDefault;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public Visibility setChangeable(boolean changeable) {
        this.changeable = changeable;
        return this;
    }

    public boolean isHiddenByDefault() {
        return hiddenByDefault;
    }

    public Visibility setHiddenByDefault(boolean hiddenByDefault) {
        this.hiddenByDefault = hiddenByDefault;
        return this;
    }
}
