package me.rerere.matrix.license;

/** Values serialized into the license verification request. */
public final class VerifyRequest {
    private final String ping;
    private final String token;
    private final int product;
    private final String hardwareId;
    private final String operatingSystemName;
    private final String operatingSystemVersion;
    private final String operatingSystemArchitecture;
    private final String javaVersion;
    private final String javaVendor;
    private final String userName;
    private final String userHome;

    public VerifyRequest(String ping, String token, int product, String hardwareId,
                         String operatingSystemName, String operatingSystemVersion,
                         String operatingSystemArchitecture, String javaVersion,
                         String javaVendor, String userName, String userHome) {
        this.ping = ping;
        this.token = token;
        this.product = product;
        this.hardwareId = hardwareId;
        this.operatingSystemName = operatingSystemName;
        this.operatingSystemVersion = operatingSystemVersion;
        this.operatingSystemArchitecture = operatingSystemArchitecture;
        this.javaVersion = javaVersion;
        this.javaVendor = javaVendor;
        this.userName = userName;
        this.userHome = userHome;
    }

    public VerifyRequest copy(String ping, String token, int product, String hardwareId,
                              String operatingSystemName, String operatingSystemVersion,
                              String operatingSystemArchitecture, String javaVersion,
                              String javaVendor, String userName, String userHome) {
        return new VerifyRequest(ping, token, product, hardwareId, operatingSystemName,
                operatingSystemVersion, operatingSystemArchitecture, javaVersion,
                javaVendor, userName, userHome);
    }

    public String getPing() { return ping; }
    public String pingValue() { return ping; }
    public String getToken() { return token; }
    public String tokenValue() { return token; }
    public int getProduct() { return product; }
    public int productValue() { return product; }
    public String getHardwareId() { return hardwareId; }
    public String hardwareIdValue() { return hardwareId; }
    public String getOperatingSystemName() { return operatingSystemName; }
    public String operatingSystemNameValue() { return operatingSystemName; }
    public String getOperatingSystemVersion() { return operatingSystemVersion; }
    public String operatingSystemVersionValue() { return operatingSystemVersion; }
    public String getOperatingSystemArchitecture() { return operatingSystemArchitecture; }
    public String operatingSystemArchitectureValue() { return operatingSystemArchitecture; }
    public String getJavaVersion() { return javaVersion; }
    public String javaVersionValue() { return javaVersion; }
    public String getJavaVendor() { return javaVendor; }
    public String javaVendorValue() { return javaVendor; }
    public String getUserName() { return userName; }
    public String userNameValue() { return userName; }
    public String getUserHome() { return userHome; }
    public String userHomeValue() { return userHome; }

    @Override
    public String toString() {
        return "VerifyRequest(ping=" + ping + ", token=" + token
                + ", product=" + product + ", hwid=" + hardwareId
                + ", osName=" + operatingSystemName + ", osVersion=" + operatingSystemVersion
                + ", osArch=" + operatingSystemArchitecture + ", javaVersion=" + javaVersion
                + ", javaVendor=" + javaVendor + ", userName=" + userName
                + ", userHome=" + userHome + ')';
    }

    @Override
    public int hashCode() {
        int result = ping.hashCode();
        result = result * 31 + token.hashCode();
        result = result * 31 + Integer.hashCode(product);
        result = result * 31 + hardwareId.hashCode();
        result = result * 31 + operatingSystemName.hashCode();
        result = result * 31 + operatingSystemVersion.hashCode();
        result = result * 31 + operatingSystemArchitecture.hashCode();
        result = result * 31 + javaVersion.hashCode();
        result = result * 31 + javaVendor.hashCode();
        result = result * 31 + userName.hashCode();
        return result * 31 + userHome.hashCode();
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof VerifyRequest)) return false;
        VerifyRequest other = (VerifyRequest) candidate;
        return java.util.Objects.equals(ping, other.ping)
                && java.util.Objects.equals(token, other.token)
                && product == other.product
                && java.util.Objects.equals(hardwareId, other.hardwareId)
                && java.util.Objects.equals(operatingSystemName, other.operatingSystemName)
                && java.util.Objects.equals(operatingSystemVersion, other.operatingSystemVersion)
                && java.util.Objects.equals(operatingSystemArchitecture, other.operatingSystemArchitecture)
                && java.util.Objects.equals(javaVersion, other.javaVersion)
                && java.util.Objects.equals(javaVendor, other.javaVendor)
                && java.util.Objects.equals(userName, other.userName)
                && java.util.Objects.equals(userHome, other.userHome);
    }
}
