package hudson.plugins.ec2;

import static hudson.plugins.ec2.ConnectionStrategy.*;

import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.services.ec2.model.Instance;

public class EC2HostAddressProvider {
    public static String unix(Instance instance, ConnectionStrategy strategy) {
        switch (strategy) {
            case PUBLIC_DNS:
                return filterNonEmpty(getPublicDnsName(instance)).orElse(getPublicIpAddress(instance));
            case PUBLIC_IP:
                return getPublicIpAddress(instance);
            case PRIVATE_DNS:
                return filterNonEmpty(getPrivateDnsName(instance)).orElse(getPrivateIpAddress(instance));
            case PRIVATE_IP:
                return getPrivateIpAddress(instance);
            default:
                throw new IllegalArgumentException("Could not unix host address for strategy = " + strategy.toString());
        }
    }

    public static String mac(Instance instance, ConnectionStrategy strategy) {
        switch (strategy) {
            case PUBLIC_DNS:
                return filterNonEmpty(getPublicDnsName(instance)).orElse(getPublicIpAddress(instance));
            case PUBLIC_IP:
                return getPublicIpAddress(instance);
            case PRIVATE_DNS:
                return filterNonEmpty(getPrivateDnsName(instance)).orElse(getPrivateIpAddress(instance));
            case PRIVATE_IP:
                return getPrivateIpAddress(instance);
            default:
                throw new IllegalArgumentException("Could not mac host address for strategy = " + strategy.toString());
        }
    }

    public static String windows(Instance instance, ConnectionStrategy strategy) {
        if (strategy.equals(PRIVATE_DNS) || strategy.equals(PRIVATE_IP)) {
            return getPrivateIpAddress(instance);
        } else if (strategy.equals(PUBLIC_DNS) || strategy.equals(PUBLIC_IP)) {
            return getPublicIpAddress(instance);
        } else {
            throw new IllegalArgumentException("Could not windows host address for strategy = " + strategy.toString());
        }
    }

    private static String getPublicDnsName(Instance instance) {
        return instance.publicDnsName();
    }

    private static String getPublicIpAddress(Instance instance) {
        return instance.publicIpAddress();
    }

    private static String getPrivateDnsName(Instance instance) {
        return instance.privateDnsName();
    }

    private static String getPrivateIpAddress(Instance instance) {
        return instance.privateIpAddress();
    }

    private static Optional<String> filterNonEmpty(String value) {
        return Optional.ofNullable(value).filter(StringUtils::isNotEmpty);
    }
}
