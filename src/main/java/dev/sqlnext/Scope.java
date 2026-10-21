package dev.sqlnext;

import java.net.URI;
import java.util.Locale;

/** Exact origin and path-segment boundary scope. No DNS or network activity. */
public final class Scope {
    private Scope() {}
    public static URI parse(String value) {
        try {
            URI u = new URI(value.strip());
            String scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                    || u.getHost() == null || u.getRawUserInfo() != null || u.getRawFragment() != null
                    || u.getPort() > 65535 || u.getPort() == 0
                    || u.getRawPath().contains("%") || u.getRawPath().contains("\\")
                    || !u.normalize().getPath().equals(u.getPath()))
                throw new IllegalArgumentException();
            return u;
        } catch (Exception e) { throw new IllegalArgumentException("Use an absolute HTTP(S) URL without credentials, fragments, encoded paths or dot segments."); }
    }
    private static int port(URI u) { return u.getPort() == -1 ? (u.getScheme().equalsIgnoreCase("https") ? 443 : 80) : u.getPort(); }
    public static boolean allows(String approved, String target) {
        URI a = parse(approved), t = parse(target);
        String path = a.getPath().replaceAll("/+$", "");
        return a.getScheme().equalsIgnoreCase(t.getScheme()) && a.getHost().toLowerCase(Locale.ROOT).equals(t.getHost().toLowerCase(Locale.ROOT))
                && port(a) == port(t) && (t.getPath().equals(path) || t.getPath().startsWith(path + "/"));
    }
}
