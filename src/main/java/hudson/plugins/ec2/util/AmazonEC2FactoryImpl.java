package hudson.plugins.ec2.util;

import hudson.Extension;
import hudson.plugins.ec2.AmazonEC2Cloud;
import hudson.plugins.ec2.EC2Cloud;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;

@Extension
public class AmazonEC2FactoryImpl implements AmazonEC2Factory {

    // Will use the alternate EC2 endpoint if provided by the UI (via a @QueryParameter field), or use the default
    // value if not specified.
    // VisibleForTesting
    public static URL determineEC2EndpointURL(URL altEC2Endpoint, software.amazon.awssdk.regions.Region region)
            throws MalformedURLException {
        if (altEC2Endpoint == null) {
            if (region == null) {
                return new URL(DEFAULT_EC2_ENDPOINT);
            }
            return AmazonEC2Cloud.getEc2EndpointUrl(region.toString());
        }
        return altEC2Endpoint;
    }

    @Override
    public Ec2Client connect(
            AwsCredentialsProvider credentialsProvider, String ec2EndpointOverride, String regionName) {
        URL url;
        try {
            url = new URL(ec2EndpointOverride);
        } catch (MalformedURLException e) {
            LOGGER.log(
                    Level.WARNING,
                    "The alternate EC2 endpoint is malformed ({0}). Using the default endpoint ({1})",
                    new Object[] {ec2EndpointOverride, DEFAULT_EC2_ENDPOINT});
            url = null;
        }
        return connect(credentialsProvider, url, regionName);
    }

    @Override
    public Ec2Client connect(AwsCredentialsProvider credentialsProvider, URL ec2EndpointOverride, String regionName) {
        Ec2Client client;
        software.amazon.awssdk.regions.Region region =
                regionName == null ? null : software.amazon.awssdk.regions.Region.of(regionName);
        try {
            URL ec2Endpoint = determineEC2EndpointURL(ec2EndpointOverride, region);
            Ec2ClientBuilder clientBuilder = Ec2Client.builder()
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(ec2Endpoint.toURI())
                    .httpClient(EC2Cloud.createHttpClient(ec2Endpoint.getHost()))
                    .overrideConfiguration(EC2Cloud.createClientOverrideConfiguration());
            if (region != null) {
                clientBuilder.region(region);
            }
            client = clientBuilder.build();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        return client;
    }
}
