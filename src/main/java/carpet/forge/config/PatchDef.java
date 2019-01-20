package carpet.forge.config;

import net.minecraft.network.NetHandlerPlayServer;
import carpet.forge.CarpetMain;
import carpet.forge.core.CarpetCore;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PatchDef {
    public enum Side {SERVER, CLIENT, JOINED}

    public boolean loaded;
    public boolean enabled;
    private boolean serverEnabled;

    private final String fieldName;
    private final Enum<Side> side;

    private String displayName;
    private String category;
    private String credits;
    private String sideEffects;
    private String[] comment;
    private boolean toggleable = true;
    private boolean clientToggleable = false;
    private boolean[] defaults;

    public PatchDef(String fieldNameIn, Enum<Side> sideIn) {
        this(fieldNameIn, sideIn, ServerSyncHandlers.ENFORCE, ClientSyncHandlers.IGNORE);
    }

    public PatchDef(String fieldNameIn, Enum<Side> sideIn, BiConsumer<PatchDef, Boolean> serverSyncHandler) {
        this(fieldNameIn, sideIn, serverSyncHandler, ClientSyncHandlers.IGNORE);
    }

    public PatchDef(String fieldNameIn, Enum<Side> sideIn, BiConsumer<PatchDef, Boolean> serverSyncHandler, TriFunction<PatchDef, Boolean, NetHandlerPlayServer, Boolean> clientSyncHandler) {
        this.fieldName = fieldNameIn;
        this.processServerSync = serverSyncHandler;
        this.processClientSync = clientSyncHandler;
        this.side = sideIn;
        this.defaults = new boolean[]{true, true};
    }

    public final BiConsumer<PatchDef, Boolean> processServerSync;
    public final TriFunction<PatchDef, Boolean, NetHandlerPlayServer, Boolean> processClientSync;

    public boolean isLoaded() {
        return this.loaded;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public Enum<Side> getSide() {
        return side;
    }

    public void setLoaded() {
        // Can only set to true. Bug fixes can only be loaded at startup and cannot be unloaded
        this.loaded = true;
    }

    public boolean isClientToggleable() {
        return this.clientToggleable;
    }

    public PatchDef setClientToggleable(boolean clientToggleableIn) {
        this.clientToggleable = clientToggleableIn;
        return this;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public PatchDef setEnabled(boolean isEnabled) {
        this.enabled = !this.toggleable || (isEnabled && this.loaded);
        return this;
    }

    public boolean isServerEnabled() {
        return this.serverEnabled;
    }

    public PatchDef setServerEnabled(boolean isServerEnabled) {
        this.serverEnabled = isServerEnabled;
        return this;
    }

    public boolean isToggleable() {
        return this.toggleable;
    }

    public PatchDef setToggleable(boolean isToggleable) {
        this.toggleable = isToggleable;
        this.enabled = !isToggleable || this.enabled;
        return this;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public PatchDef setDisplayName(String displayNameIn) {
        this.displayName = displayNameIn;
        return this;
    }

    public String getCredits() {
        return this.credits;
    }

    public PatchDef setCredits(String creditsIn) {
        this.credits = creditsIn;
        return this;
    }

    public String getSideEffects() {
        return this.sideEffects;
    }

    public PatchDef setSideEffects(String sideEffectsIn) {
        this.sideEffects = sideEffectsIn;
        return this;
    }

    public String getCategory() {
        return this.category;
    }

    public PatchDef setCategory(String categoryIn) {
        this.category = categoryIn;
        return this;
    }

    public String[] getComment() {
        return this.comment;
    }

    public PatchDef setComment(String[] commentIn) {
        this.comment = commentIn;
        return this;
    }

    public boolean[] getDefaults() {
        return this.defaults;
    }

    public PatchDef setDefaults(boolean[] defaultsIn) {
        this.defaults = defaultsIn;
        return this;
    }

    public boolean wasLoaded() {
        Field coreField;

        try {
            coreField = CarpetCore.config.getClass().getField(this.getFieldName());
            return coreField.getBoolean(CarpetCore.config);
        } catch (Exception e) {
            CarpetMain.logger.error("Failure to fetch core config field for field name: " + this.getFieldName());
            return false;
        }
    }

    public static class ServerSyncHandlers {
        public static final BiConsumer<PatchDef, Boolean> IGNORE = (bug, enabled) -> {
        };

        public static final BiConsumer<PatchDef, Boolean> TOGGLE = (bug, enabled) -> {
            if (CarpetMain.config.isServerLocked()) {
                bug.setClientToggleable(enabled);
                if (!enabled) bug.setEnabled(false);
            } else {
                bug.setClientToggleable(true);
            }
        };

        public static final BiConsumer<PatchDef, Boolean> ENFORCE = (bug, enabled) -> {
            if (enabled && !bug.isLoaded()) {
                CarpetMain.logger.warn("Server requested to enable unloaded patch: " + bug.getDisplayName());
                return;
            }

            if (enabled && bug.isLoaded() && !bug.isEnabled()) {
                CarpetMain.logger.info("Server force enabled a disabled patch: " + bug.getDisplayName());
            }

            if (!enabled && bug.isLoaded() && bug.isEnabled()) {
                CarpetMain.logger.info("Server force disabled an enabled patch: " + bug.getDisplayName());
            }

            bug.setEnabled(enabled);
        };

        public static final BiConsumer<PatchDef, Boolean> ACCEPT = (bug, enabled) -> {
            TOGGLE.accept(bug, enabled);
            ENFORCE.accept(bug, enabled);
        };
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {

        R apply(A a, B b, C c);

        default <V> TriFunction<A, B, C, V> andThen(
                Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }
    }

    public static class ClientSyncHandlers {
        public static final TriFunction<PatchDef, Boolean, NetHandlerPlayServer, Boolean> IGNORE = (bug, enabled, handler) -> false;
    }
}