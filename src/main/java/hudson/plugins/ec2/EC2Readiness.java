package hudson.plugins.ec2;

import software.amazon.awssdk.core.exception.SdkException;

public interface EC2Readiness {
    boolean isReady();

    public String getEc2ReadinessStatus() throws SdkException;
}
