package io.honeybadger.reporter.dto;

import com.jcabi.manifests.Manifests;

import java.io.Serializable;

/**
 * Notifier section of an error reported to the Honeybadger API.
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.9
 */
public class Notifier implements Serializable {
    public static final String VERSION;

    static {
        VERSION = findVersion();
    }

    private static final long serialVersionUID = -9160493241433298708L;

    public final String name = "io.honeybadger:honeybadger-java";
    public final String url = "https://github.com/honeybadger-io/honeybadger-java";
    public final String version = VERSION;

    public Notifier() {
    }

    /**
     * Finds the version of the library from the JAR manifest or a system property.
     * @return Sting containing version number or "unknown" if we can't locate the version
     */
    private static String findVersion() {
        final String sysPropVersion = System.getProperty("honeybadger.version");

        if (sysPropVersion != null && !sysPropVersion.isEmpty()) {
            return sysPropVersion;
        }

        try {
            final String manifestVersion = Manifests.read("Honeybadger-Java-Version");

            if (manifestVersion != null && !manifestVersion.isEmpty()) {
                return manifestVersion;
            } else {
                return "unknown";
            }
        } catch (IllegalArgumentException e) {
            // We are probably running in test mode in an IDE
            return "unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notifier notifier = (Notifier) o;

        if (name != null ? !name.equals(notifier.name) : notifier.name != null) return false;
        if (url != null ? !url.equals(notifier.url) : notifier.url != null) return false;
        return !(version != null ? !version.equals(notifier.version) : notifier.version != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
