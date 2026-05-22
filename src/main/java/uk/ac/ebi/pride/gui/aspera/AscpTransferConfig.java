package uk.ac.ebi.pride.gui.aspera;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

/**
 * Transfer parameters mapped from setting.prop to ascp CLI flags (ascp 4.x).
 */
public class AscpTransferConfig {

    private int tcpPort = 33001;
    private int udpPort = 33001;
    private int targetRateKbps = 950_000;
    private int minimumRateKbps = 1_000;
    private String policy = "fair";
    private String token;
    private boolean createPath = true;
    private String overwrite = "diff";
    private int resumeLevel = 3;
    private boolean preserveTimestamps = true;

    /** Default transfer settings (no GUI / App context required). */
    public static AscpTransferConfig defaults() {
        AscpTransferConfig config = new AscpTransferConfig();
        config.token = "PRIDE-Aspera-1-Token";
        return config;
    }

    public static AscpTransferConfig fromAppContext() {
        DesktopContext ctx = App.getInstance().getDesktopContext();
        AscpTransferConfig config = defaults();
        config.tcpPort = parseInt(ctx.getProperty("aspera.xfer.tcpPort"), 33001);
        config.udpPort = parseInt(ctx.getProperty("aspera.xfer.udpPort"), 33001);
        config.targetRateKbps = parseInt(ctx.getProperty("aspera.xfer.targetRateKbps"), 950_000);
        config.minimumRateKbps = parseInt(ctx.getProperty("aspera.xfer.minimumRateKbps"), 1_000);
        String policyProp = ctx.getProperty("aspera.xfer.policy");
        if (policyProp != null && !policyProp.isBlank()) {
            config.policy = policyProp.trim().toLowerCase();
        }
        config.token = ctx.getProperty("aspera.xfer.token");
        config.createPath = Boolean.parseBoolean(ctx.getProperty("aspera.xfer.createPath"));
        return config;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public int getTargetRateKbps() {
        return targetRateKbps;
    }

    public int getMinimumRateKbps() {
        return minimumRateKbps;
    }

    public String getPolicy() {
        return policy;
    }

    public String getToken() {
        return token;
    }

    public boolean isCreatePath() {
        return createPath;
    }

    public String getOverwrite() {
        return overwrite;
    }

    public int getResumeLevel() {
        return resumeLevel;
    }

    public boolean isPreserveTimestamps() {
        return preserveTimestamps;
    }
}
