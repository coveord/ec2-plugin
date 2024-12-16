package hudson.plugins.ec2.util;

import software.amazon.awssdk.services.ec2.model.KeyPairInfo;

public class KeyPair {
    private final KeyPairInfo keyPairInfo;
    private final String material;


    public KeyPair(KeyPairInfo keyPairInfo, String material) {
        this.keyPairInfo = keyPairInfo;
        this.material = material;
    }

    public KeyPairInfo getKeyPairInfo() {
        return keyPairInfo;
    }
    public String getMaterial() {
        return material;
    }
}
