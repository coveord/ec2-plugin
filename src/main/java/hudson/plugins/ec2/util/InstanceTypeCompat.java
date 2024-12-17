package hudson.plugins.ec2.util;

import org.kohsuke.stapler.DataBoundConstructor;
import software.amazon.awssdk.services.ec2.model.InstanceType;

public class InstanceTypeCompat {
    private final InstanceType instanceType;

    @DataBoundConstructor
    public InstanceTypeCompat(String instanceType) {
        // Attempt to find correct AWS SDK Java v2 instance type from instance type string
        // Accept the value of the enum, e.g. m1.large
        InstanceType foundInstanceType = InstanceType.fromValue(instanceType);
        if (foundInstanceType == InstanceType.UNKNOWN_TO_SDK_VERSION) {
            // Also accept the name of the Enum in the AWS SDK v1, e.g. M1Large
            // Name of the enum in AWS SDK v2 is different, e.g. M1_Large
            com.amazonaws.services.ec2.model.InstanceType oldInstanceType = com.amazonaws.services.ec2.model.InstanceType.valueOf(instanceType);
            foundInstanceType = InstanceType.fromValue(oldInstanceType.toString());
        }
        this.instanceType = foundInstanceType;
    }

    public InstanceTypeCompat(com.amazonaws.services.ec2.model.InstanceType instanceType) {
        // Attempt to find correct AWS SDK Java v2 instance type from SDK v1 instance type
        this.instanceType = InstanceType.fromValue(instanceType.toString());
    }

    public InstanceType getInstanceType() {
        return this.instanceType;
    }
}
