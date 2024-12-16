package hudson.plugins.ec2.util;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;
import hudson.Extension;
import hudson.plugins.ec2.EC2Cloud;

import java.net.URI;
import java.net.URL;

@Extension
public class AmazonEC2FactoryImpl implements AmazonEC2Factory {

    @Override
    public Ec2Client connect(AwsCredentialsProvider credentialsProvider, URI ec2Endpoint) {
        Ec2Client client =
                Ec2Client.builder().credentialsProvider(credentialsProvider).endpointOverride(ec2Endpoint)
                        .build();
        return client;
    }
}
