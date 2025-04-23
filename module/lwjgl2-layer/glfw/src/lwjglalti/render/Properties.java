package lwjglalti.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

public class Properties {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final java.util.Properties delegate = new java.util.Properties();

    static {
        InputStream stream = Properties.class.getClassLoader().getResourceAsStream("lwjgl-altitude.properties");
        if (stream == null) {
            LOG.error("lwjgl-altitude properties not found on classpath");
        } else {
            try {
                delegate.load(stream);
            } catch (FileNotFoundException __) {
                // this is fine
            } catch (IOException e) {
                LOG.error("Failed to load lwjgl-altitude properties", e);
            }
        }
    }

    private static final boolean PREFER_WINDOWED_FULLSCREEN =
            Boolean.parseBoolean(delegate.getProperty("prefer_windowed_fullscreen"));

    private Properties() {
        // static api
    }

    public static boolean preferWindowedFullscreen() {
        return PREFER_WINDOWED_FULLSCREEN;
    }
}
