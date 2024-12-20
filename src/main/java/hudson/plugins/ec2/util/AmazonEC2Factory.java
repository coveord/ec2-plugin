package hudson.plugins.ec2.util;

import hudson.ExtensionPoint;
import java.net.URL;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;

public interface AmazonEC2Factory extends ExtensionPoint {
    Logger LOGGER = Logger.getLogger(AmazonEC2Factory.class.getName());

    String DEFAULT_EC2_ENDPOINT = "https://ec2.amazonaws.com";

    static AmazonEC2Factory getInstance() {
        AmazonEC2Factory instance = null;
        for (AmazonEC2Factory implementation : Jenkins.get().getExtensionList(AmazonEC2Factory.class)) {
            if (instance != null) {
                throw new IllegalStateException("Multiple implementations of " + AmazonEC2Factory.class.getName()
                        + " found. If overriding, please consider using ExtensionFilter");
            }
            instance = implementation;
        }
        return instance;
    }

    public Ec2Client connect(AwsCredentialsProvider credentialsProvider, URL ec2EndpointOverride, String regionName);

    public Ec2Client connect(AwsCredentialsProvider credentialsProvider, String ec2EndpointOverride, String regionName);
}
