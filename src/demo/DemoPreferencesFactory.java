package demo;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/** Isolated settings, persisted only inside the selected rehearsal profile. */
public final class DemoPreferencesFactory implements PreferencesFactory {
    private final Store store = new Store();
    private final Preferences root = new MemoryNode(null, "", store);
    public Preferences userRoot() { return root; }
    public Preferences systemRoot() { return root; }

    private static final class MemoryNode extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<String, String>();
        private final Map<String, MemoryNode> children = new HashMap<String, MemoryNode>();
        private final Store store;
        MemoryNode(AbstractPreferences parent, String name, Store store) {
            super(parent, name); this.store = store;
            String prefix = absolutePath() + ":";
            for (String key : store.properties.stringPropertyNames()) {
                if (key.startsWith(prefix)) values.put(key.substring(prefix.length()), store.properties.getProperty(key));
            }
        }
        protected void putSpi(String key, String value) {
            values.put(key, value); store.properties.setProperty(absolutePath() + ":" + key, value); store.save();
        }
        protected String getSpi(String key) { return values.get(key); }
        protected void removeSpi(String key) {
            values.remove(key); store.properties.remove(absolutePath() + ":" + key); store.save();
        }
        protected void removeNodeSpi() { values.clear(); }
        protected String[] keysSpi() { return values.keySet().toArray(new String[0]); }
        protected String[] childrenNamesSpi() { return children.keySet().toArray(new String[0]); }
        protected AbstractPreferences childSpi(String name) {
            MemoryNode node = children.get(name);
            if (node == null || node.isRemoved()) {
                node = new MemoryNode(this, name, store);
                children.put(name, node);
            }
            return node;
        }
        protected void syncSpi() { }
        protected void flushSpi() { store.save(); }
    }

    private static final class Store {
        final java.util.Properties properties = new java.util.Properties();
        final java.nio.file.Path path;
        Store() {
            String directory = System.getProperty("invaders.demo.profile");
            path = directory == null ? null : java.nio.file.Paths.get(directory).resolve("settings.properties");
            try {
                if (path != null && java.nio.file.Files.exists(path))
                    try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(path)) { properties.load(reader); }
            } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot load demo settings", exception); }
        }
        synchronized void save() {
            if (path == null) return;
            try {
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Path temporary = java.nio.file.Files.createTempFile(path.getParent(), "settings-", ".tmp");
                try {
                    try (java.io.Writer writer = java.nio.file.Files.newBufferedWriter(temporary)) {
                        properties.store(writer, "Presentation settings only");
                    }
                    java.nio.file.Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } finally { java.nio.file.Files.deleteIfExists(temporary); }
            } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot save demo settings", exception); }
        }
    }
}
