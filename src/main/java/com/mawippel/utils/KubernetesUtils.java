package com.mawippel.utils;

public class KubernetesUtils {

    /**
     * Returns the pod IP address following the K8s pattern.
     * For example, if a Pod in the default namespace has the IP address 172.17.0.3, and the domain name for
     * your cluster is cluster.local, then the Pod has a DNS name: 172-17-0-3.default.pod.cluster.local
     */
    public static String getIpAddressInKubernetesPattern(String podIp) {
        // Replace all . with - according to the k8s docs (https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/#a-aaaa-records-1)
        return podIp.replaceAll("\\.", "-");
    }

    public static String buildDNSEntry(String namespace, String podIp) {
        return "%s.%s.pod.cluster.local".formatted(podIp, namespace);
    }
}
