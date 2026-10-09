package me.rerere.matrix.crypto;

import java.util.Base64;

/** Immutable accessors for the ping-key material embedded in the canonical class. */
public final class PingKeyMaterial {
    private PingKeyMaterial() { }

    public static byte[] cipherBytes() {
        return Base64.getDecoder().decode("FZMFlcMCmvToFdfL554xoCHeBsJ0GMwMDMuYiSxButdLgmOdq3pZmtT2H4IQ76aB5n7qTVZms2RliIpNS/GgWE9R5ajsEXyQwZNZSwxaoS2p22jXDlgf20Fa4XuyKy28WgvoTiB34/Zt92KAl6fuLK2LHAEVFyAb3O1qyj754FRpbesOV0ZM03HWv/96zf446YWg6AJyCKbcLbHrLYA2fxsJ5wx3YJw5vkSZWxUw+QkqrHiiUlFMOplhczRIfB7UJL1QfcvMJCktI2U4h61pDA57gUZk7SFY8u9ZkAcvcSqnSm2bW2MtGHPSqmcOXoVFDnc+8Rmw6X9RPllh61G0pN81iDNtBlm/ZIQnV4tSUWqoteg3Z692BKrt3f+8z+tlp5yhJVsQcsCbmYERrNbCgyS8jrOrcILVnlSd5FX8DShw+SPHHoDHnX21KFg/MWRhaHPe4URShRFioYn/kp/EFImPCJJ6bplm2ZJSwP5q+5UVT9ufj4NQRBC+xQq90flMx2eKmBEkvbc=" );
    }

    public static byte[] saltBytes() {
        return Base64.getDecoder().decode("O4PZykLFu3lE6TyKBaosQg==");
    }

    public static int keyId() { return 194; }
}
